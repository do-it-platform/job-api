package de.doit.jobapi.web

import de.doit.jobapi.domain.event.JobDataRecord
import de.doit.jobapi.domain.event.JobPostedEvent
import de.doit.jobapi.domain.event.JobUpdatedEvent
import de.doit.jobapi.domain.model.*
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ObjectAssert
import org.jeasy.random.EasyRandom
import org.junit.jupiter.api.*
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.time.Duration.ofSeconds

@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = ["spring.kafka.bootstrap-servers=\${spring.embedded.kafka.brokers}"])
@EmbeddedKafka(topics = ["\${japi.kafka.topic}"])
class JobResourceIntegrationTest {

    @Autowired lateinit var client: WebTestClient
    @Autowired lateinit var easyRandom: EasyRandom
    @Autowired lateinit var kafkaBroker: EmbeddedKafkaBroker
    @Autowired lateinit var jobEventConsumerFactory: ConsumerFactory<String, GenericRecord>
    @Value("\${japi.kafka.topic}") private lateinit var topic: String

    @Nested
    @DisplayName("With kafka available")
    inner class WithKafkaAvailable {

        private lateinit var jobEventConsumer: Consumer<String, GenericRecord>

        @BeforeEach
        internal fun setUp() {
            jobEventConsumer = jobEventConsumerFactory.createConsumer("test-group", null)
            kafkaBroker.consumeFromAnEmbeddedTopic(jobEventConsumer, topic)
        }

        @AfterEach
        internal fun tearDown() {
            jobEventConsumer.commitSync()
            jobEventConsumer.close()
        }

        @Nested
        @DisplayName("POST /jobs")
        inner class PostJobs {

            @Test
            @DisplayName("should publish job-posted event")
            fun postJobShouldPublishJobPostedEvent() {
                val userId = "2342-56456"
                val jobInputData = easyRandom.nextObject(JobData::class.java)

                client.post()
                        .uri("/jobs")
                        .header("X-User-Id", userId)
                        .contentType(APPLICATION_JSON)
                        .bodyValue(jobInputData)
                        .accept(APPLICATION_JSON)
                        .exchange()
                        .expectStatus().isCreated
                        .expectBody<JobDTO>()
                        .consumeWith {
                            val jobOutputData = it.responseBody
                            assertThat(jobOutputData).isNotNull
                            val jobPostedEvent = consumeLastJobEvent()
                            assertThat(jobPostedEvent.key()).isEqualTo(jobOutputData!!.id.value)
                            assertThat(jobPostedEvent.value())
                                    .isInstanceOf(JobPostedEvent::class.java)
                                    .isEqualTo(jobPostedEvent(jobInputData, jobOutputData.id, userId))
                        }
            }

            @Test
            @DisplayName("should return dto after success")
            fun postJobShouldReturnDtoAfterSuccessfullyPublishedEvent() {
                val jobInputData = easyRandom.nextObject(JobData::class.java)

                client.post()
                        .uri("/jobs")
                        .header("X-User-Id", "1234")
                        .contentType(APPLICATION_JSON)
                        .bodyValue(jobInputData)
                        .accept(APPLICATION_JSON)
                        .exchange()
                        .expectStatus().isCreated
                        .expectBody<JobDTO>()
                        .consumeWith {
                            val jobOutputData = it.responseBody
                            assertThat(jobOutputData).isNotNull
                            assertThat(jobOutputData!!.id.value).isNotNull().isNotBlank()
                            assertThat(jobOutputData).containsValuesOf(jobInputData)
                        }

                assertThat(consumeJobEvents()).hasSize(1)
            }
        }

        @Nested
        @DisplayName("PUT /jobs/{id}")
        inner class PutJobs {

            private val userId = "1234"
            private var existingJobId: JobId? = null

            @BeforeEach
            internal fun setUp() {
                val jobInputData = easyRandom.nextObject(JobData::class.java)

                client.post()
                        .uri("/jobs")
                        .header("X-User-Id", userId)
                        .contentType(APPLICATION_JSON)
                        .bodyValue(jobInputData)
                        .accept(APPLICATION_JSON)
                        .exchange()
                        .expectStatus().isCreated
                        .expectBody<JobDTO>()
                        .consumeWith {
                            existingJobId = it.responseBody!!.id
                        }

                consumeJobEvents()
            }

            @Test
            @DisplayName("should return not found when given id not exists")
            internal fun putJobsShouldReturnNotFoundIfGivenIdNotExists() {
                val updatedJobData = easyRandom.nextObject(JobData::class.java)

                client.put()
                        .uri("/jobs/{id}", "not-existing-id")
                        .header("X-User-Id", userId)
                        .contentType(APPLICATION_JSON)
                        .bodyValue(updatedJobData)
                        .accept(APPLICATION_JSON)
                        .exchange()
                        .expectStatus().isNotFound

                assertThat(consumeJobEvents()).isEmpty()
            }

            @Test
            @DisplayName("should publish job-updated event")
            internal fun putJobsShouldPublishJobUpdatedEvent() {
                val updatedJobData = easyRandom.nextObject(JobData::class.java)

                client.put()
                        .uri("/jobs/{id}", existingJobId?.value)
                        .header("X-User-Id", userId)
                        .contentType(APPLICATION_JSON)
                        .bodyValue(updatedJobData)
                        .accept(APPLICATION_JSON)
                        .exchange()
                        .expectStatus().isNoContent
                        .expectBody().isEmpty

                val jobUpdatedEvent = consumeLastJobEvent()
                assertThat(jobUpdatedEvent.key()).isEqualTo(existingJobId?.value)
                assertThat(jobUpdatedEvent.value())
                        .isInstanceOf(JobUpdatedEvent::class.java)
                        .isEqualTo(jobUpdatedEvent(updatedJobData, existingJobId!!, userId))
            }

            @Test
            @DisplayName("should return forbidden when given id does not belong to user")
            internal fun putJobsShouldReturnForbiddenWhenGivenIdNotBelongsToUser() {
                val updatedJobData = easyRandom.nextObject(JobData::class.java)

                client.put()
                        .uri("/jobs/{id}", existingJobId?.value)
                        .header("X-User-Id", "foreign-user-id")
                        .contentType(APPLICATION_JSON)
                        .bodyValue(updatedJobData)
                        .accept(APPLICATION_JSON)
                        .exchange()
                        .expectStatus().isForbidden

                assertThat(consumeJobEvents()).isEmpty()
            }

        }

        private fun consumeJobEvents(): ConsumerRecords<String, GenericRecord> {
            return KafkaTestUtils.getRecords(jobEventConsumer, ofSeconds(2).toMillis())
        }

        private fun consumeLastJobEvent(): ConsumerRecord<String, GenericRecord> {
            val records = consumeJobEvents()
            assertThat(records).hasSize(1)
            return records.first()
        }

    }

    @Nested
    @DisplayName("Without kafka available")
    @TestInstance(PER_CLASS)
    inner class WithoutKafkaAvailable {

        private lateinit var existingJobId: String

        @BeforeAll
        internal fun setUp() {
            val jobEventConsumer = jobEventConsumerFactory.createConsumer("test-group-2", null)
            kafkaBroker.consumeFromAnEmbeddedTopic(jobEventConsumer, topic)
            existingJobId = KafkaTestUtils.getRecords(jobEventConsumer, ofSeconds(2).toMillis()).last().key()
            jobEventConsumer.close()

            kafkaBroker.kafkaServers.forEach {
                it.shutdown()
                it.awaitShutdown()
            }
        }

        @Nested
        @DisplayName("POST /jobs")
        inner class PostJobs {

            @Test
            @DisplayName("should return server error")
            fun postJobShouldReturnErrorWhenKafkaIsNotAvailable() {
                val jobInputData = easyRandom.nextObject(JobData::class.java)

                client.post()
                        .uri("/jobs")
                        .header("X-User-Id", "1234")
                        .contentType(APPLICATION_JSON)
                        .bodyValue(jobInputData)
                        .accept(APPLICATION_JSON)
                        .exchange()
                        .expectStatus().is5xxServerError
            }

        }

        @Nested
        @DisplayName("PUT /jobs/{id}")
        inner class PutJobs {

            @Test
            @DisplayName("should return server error")
            fun postJobShouldReturnErrorWhenKafkaIsNotAvailable() {
                val jobUpdateData = easyRandom.nextObject(JobData::class.java)

                client.put()
                        .uri("/jobs/{id}", existingJobId)
                        .header("X-User-Id", "1234")
                        .contentType(APPLICATION_JSON)
                        .bodyValue(jobUpdateData)
                        .accept(APPLICATION_JSON)
                        .exchange()
                        .expectStatus().is5xxServerError
            }

        }

    }

    private fun jobPostedEvent(jobInputData: JobData, jobId: JobId, userId: String): JobPostedEvent {
        return JobPostedEvent.newBuilder()
                .setData(jobDataRecord(jobInputData, jobId, userId))
                .build()
    }

    private fun jobUpdatedEvent(jobInputData: JobData, jobId: JobId, userId: String): JobUpdatedEvent {
        return JobUpdatedEvent.newBuilder()
                .setData(jobDataRecord(jobInputData, jobId, userId))
                .build()
    }

    private fun jobDataRecord(jobInputData: JobData, jobId: JobId, userId: String): JobDataRecord {
        return JobDataRecord.newBuilder().setId(jobId.value)
                .setTitle(jobInputData.title)
                .setDescription(jobInputData.description)
                .setLocation(Location.newBuilder()
                        .setLatitude(jobInputData.latitude)
                        .setLongitude(jobInputData.longitude)
                        .build()
                )
                .setPayment(jobInputData.payment)
                .setVendorId(userId)
                .build()
    }

    private fun ObjectAssert<JobDTO>.containsValuesOf(jobData: JobData) {
        satisfies {
            it.apply {
                assertThat(title).isEqualTo(jobData.title)
                assertThat(description).isEqualTo(jobData.description)
                assertThat(latitude).isEqualTo(jobData.latitude)
                assertThat(longitude).isEqualTo(jobData.longitude)
                assertThat(payment).isEqualTo(jobData.payment.toPlainString())
            }
        }
    }

}