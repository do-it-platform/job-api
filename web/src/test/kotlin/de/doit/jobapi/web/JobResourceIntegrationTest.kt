package de.doit.jobapi.web

import de.doit.jobapi.SchemaRegistryTestConfig
import de.doit.jobapi.domain.event.JobDeletedEvent
import de.doit.jobapi.domain.event.JobPostedEvent
import de.doit.jobapi.domain.event.JobUpdatedEvent
import de.doit.jobapi.web.model.JobRequest
import de.doit.jobapi.web.model.JobResponse
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ObjectAssert
import org.jeasy.random.EasyRandom
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.time.Duration.ofSeconds

@DirtiesContext
@ActiveProfiles("test")
@AutoConfigureWebTestClient
@Import(SchemaRegistryTestConfig::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@EmbeddedKafka(topics = ["\${jobapi.kafka.topic}"])
class JobResourceIntegrationTest {

    @Autowired lateinit var client: WebTestClient
    @Autowired lateinit var easyRandom: EasyRandom
    @Autowired lateinit var kafkaBroker: EmbeddedKafkaBroker
    @Autowired lateinit var jobEventConsumerFactory: ConsumerFactory<String, GenericRecord>
    @Value("\${jobapi.kafka.topic}") private lateinit var topic: String

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
                val jobInputData = easyRandom.nextObject(JobRequest::class.java)

                client.post()
                        .uri("/jobs")
                        .header("X-User-Id", userId)
                        .contentType(APPLICATION_JSON)
                        .bodyValue(jobInputData)
                        .accept(APPLICATION_JSON)
                        .exchange()
                        .expectStatus().isCreated
                        .expectBody<JobResponse>()
                        .consumeWith {
                            val jobOutputData = it.responseBody
                            assertThat(jobOutputData).isNotNull
                            val jobPostedEvent = consumeLastJobEvent()
                            assertThat(jobPostedEvent.key()).isEqualTo(jobOutputData!!.id.value)
                            assertThat(jobPostedEvent.value()).isInstanceOf(JobPostedEvent::class.java)
                            with((jobPostedEvent.value() as JobPostedEvent).getData()) {
                                assertThat(getId()).isEqualTo(jobOutputData.id.value)
                                assertThat(getVendorId()).isEqualTo(userId)
                                assertThat(getTitle()).isEqualTo(jobInputData.title)
                                assertThat(getDescription()).isEqualTo(jobInputData.description)
                                assertThat(getLocation().getLatitude()).isEqualTo(jobInputData.latitude)
                                assertThat(getLocation().getLongitude()).isEqualTo(jobInputData.longitude)
                                assertThat(getPayment()).isEqualTo(jobInputData.payment)
                            }
                        }
            }

            @Test
            @DisplayName("should return dto after success")
            fun postJobShouldReturnDtoAfterSuccessfullyPublishedEvent() {
                val jobInputData = easyRandom.nextObject(JobRequest::class.java)

                client.post()
                        .uri("/jobs")
                        .header("X-User-Id", "1234")
                        .contentType(APPLICATION_JSON)
                        .bodyValue(jobInputData)
                        .accept(APPLICATION_JSON)
                        .exchange()
                        .expectStatus().isCreated
                        .expectBody<JobResponse>()
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
            private var jobPostedEvent: JobPostedEvent? = null

            @BeforeEach
            internal fun setUp() {
                val jobInputData = easyRandom.nextObject(JobRequest::class.java)

                client.post()
                        .uri("/jobs")
                        .header("X-User-Id", userId)
                        .contentType(APPLICATION_JSON)
                        .bodyValue(jobInputData)
                        .accept(APPLICATION_JSON)
                        .exchange()
                        .expectStatus().isCreated

                jobPostedEvent = consumeLastJobEvent().value() as JobPostedEvent
            }

            @Test
            @DisplayName("should return not found when given id not exists")
            internal fun putJobsShouldReturnNotFoundIfGivenIdNotExists() {
                val updatedJobData = easyRandom.nextObject(JobRequest::class.java)

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
                val updatedJobData = easyRandom.nextObject(JobRequest::class.java)
                val jobToUpdate = jobPostedEvent!!.getData()

                client.put()
                        .uri("/jobs/{id}", jobToUpdate.getId())
                        .header("X-User-Id", userId)
                        .contentType(APPLICATION_JSON)
                        .bodyValue(updatedJobData)
                        .accept(APPLICATION_JSON)
                        .exchange()
                        .expectStatus().isNoContent
                        .expectBody().isEmpty

                val jobUpdatedEvent = consumeLastJobEvent()
                assertThat(jobUpdatedEvent.key()).isEqualTo(jobToUpdate.getId())
                assertThat(jobUpdatedEvent.value()).isInstanceOf(JobUpdatedEvent::class.java)
                with((jobUpdatedEvent.value() as JobUpdatedEvent).getData()) {
                    assertThat(getId()).isEqualTo(jobToUpdate.getId())
                    assertThat(getVendorId()).isEqualTo(userId)
                    assertThat(getTitle()).isEqualTo(updatedJobData.title)
                    assertThat(getDescription()).isEqualTo(updatedJobData.description)
                    assertThat(getLocation().getLatitude()).isEqualTo(updatedJobData.latitude)
                    assertThat(getLocation().getLongitude()).isEqualTo(updatedJobData.longitude)
                    assertThat(getPayment()).isEqualTo(updatedJobData.payment)
                }
            }

            @Test
            @DisplayName("should return forbidden when given id does not belong to user")
            internal fun putJobsShouldReturnForbiddenWhenGivenIdNotBelongsToUser() {
                val updatedJobData = easyRandom.nextObject(JobRequest::class.java)
                val jobToUpdate = jobPostedEvent!!.getData()

                client.put()
                        .uri("/jobs/{id}", jobToUpdate.getId())
                        .header("X-User-Id", "foreign-user-id")
                        .contentType(APPLICATION_JSON)
                        .bodyValue(updatedJobData)
                        .accept(APPLICATION_JSON)
                        .exchange()
                        .expectStatus().isForbidden

                assertThat(consumeJobEvents()).isEmpty()
            }

            @Test
            @DisplayName("should return not found after delete")
            internal fun putJobsShouldReturnNotFoundAfterDelete() {
                val updatedJobData = easyRandom.nextObject(JobRequest::class.java)
                val jobToUpdate = jobPostedEvent!!.getData()

                client.delete()
                        .uri("/jobs/{id}", jobToUpdate.getId())
                        .header("X-User-Id", userId)
                        .accept(APPLICATION_JSON)
                        .exchange()
                        .expectStatus().isNoContent

                consumeJobEvents()

                client.put()
                        .uri("/jobs/{id}", jobToUpdate.getId())
                        .header("X-User-Id", userId)
                        .contentType(APPLICATION_JSON)
                        .bodyValue(updatedJobData)
                        .accept(APPLICATION_JSON)
                        .exchange()
                        .expectStatus().isNotFound

                assertThat(consumeJobEvents()).isEmpty()
            }

        }

        @Nested
        @DisplayName("DELETE /jobs/{id}")
        inner class DeleteJobs {

            private val userId = "1234"
            private var jobPostedEvent: JobPostedEvent? = null

            @BeforeEach
            internal fun setUp() {
                val jobInputData = easyRandom.nextObject(JobRequest::class.java)

                client.post()
                        .uri("/jobs")
                        .header("X-User-Id", userId)
                        .contentType(APPLICATION_JSON)
                        .bodyValue(jobInputData)
                        .accept(APPLICATION_JSON)
                        .exchange()
                        .expectStatus().isCreated

                jobPostedEvent = consumeLastJobEvent().value() as JobPostedEvent
            }

            @Test
            @DisplayName("should return not found when given id not exists")
            internal fun deleteJobsShouldReturnNotFoundIfGivenIdNotExists() {
                client.delete()
                        .uri("/jobs/{id}", "not-existing-id")
                        .header("X-User-Id", userId)
                        .accept(APPLICATION_JSON)
                        .exchange()
                        .expectStatus().isNotFound

                assertThat(consumeJobEvents()).isEmpty()
            }

            @Test
            @DisplayName("should publish job-deleted event")
            internal fun deleteJobsShouldPublishDeleteEvent() {
                val jobToDelete = jobPostedEvent!!.getData()

                client.delete()
                        .uri("/jobs/{id}", jobToDelete.getId())
                        .header("X-User-Id", userId)
                        .accept(APPLICATION_JSON)
                        .exchange()
                        .expectStatus().isNoContent
                        .expectBody().isEmpty

                val jobUpdatedEvent = consumeLastJobEvent()
                assertThat(jobUpdatedEvent.key()).isEqualTo(jobToDelete.getId())
                assertThat(jobUpdatedEvent.value()).isInstanceOf(JobDeletedEvent::class.java)
            }

            @Test
            @DisplayName("should return forbidden when given id does not belong to user")
            internal fun deleteJobsShouldReturnForbiddenWhenGivenIdNotBelongsToUser() {
                val jobToDelete = jobPostedEvent!!.getData()

                client.delete()
                        .uri("/jobs/{id}", jobToDelete.getId())
                        .header("X-User-Id", "foreign-user-id")
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

        private val userId = "12345"
        private lateinit var existingJobId: String

        @BeforeAll
        internal fun setUp() {
            val jobInputData = easyRandom.nextObject(JobRequest::class.java)

            client.post()
                    .uri("/jobs")
                    .header("X-User-Id", userId)
                    .contentType(APPLICATION_JSON)
                    .bodyValue(jobInputData)
                    .accept(APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isCreated
                    .expectBody<JobResponse>()
                    .consumeWith {
                        existingJobId = it.responseBody!!.id.value
                    }

            kafkaBroker.destroy()
        }

        @Nested
        @DisplayName("POST /jobs")
        inner class PostJobs {

            @Test
            @DisplayName("should return server error")
            fun postJobShouldReturnErrorWhenKafkaIsNotAvailable() {
                val jobInputData = easyRandom.nextObject(JobRequest::class.java)

                client.post()
                        .uri("/jobs")
                        .header("X-User-Id", userId)
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
            fun putJobShouldReturnErrorWhenKafkaIsNotAvailable() {
                val jobUpdateData = easyRandom.nextObject(JobRequest::class.java)

                client.put()
                        .uri("/jobs/{id}", existingJobId)
                        .header("X-User-Id", userId)
                        .contentType(APPLICATION_JSON)
                        .bodyValue(jobUpdateData)
                        .accept(APPLICATION_JSON)
                        .exchange()
                        .expectStatus().is5xxServerError
            }

        }

        @Nested
        @DisplayName("DELETE /jobs/{id}")
        inner class DeleteJobs {

            @Test
            @DisplayName("should return server error")
            fun deleteJobShouldReturnErrorWhenKafkaIsNotAvailable() {
                client.delete()
                        .uri("/jobs/{id}", existingJobId)
                        .header("X-User-Id", userId)
                        .accept(APPLICATION_JSON)
                        .exchange()
                        .expectStatus().is5xxServerError
            }

        }

    }

    private fun ObjectAssert<JobResponse>.containsValuesOf(jobData: JobRequest) {
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