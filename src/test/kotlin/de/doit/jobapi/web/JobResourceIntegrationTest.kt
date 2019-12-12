package de.doit.jobapi.web

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
import java.time.Duration

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
            @DisplayName("should publish job-created event")
            fun postJobShouldPublishJobCreatedEvent() {
                val userId = "2342-56456"
                val jobInputData = easyRandom.nextObject(JobData::class.java)

                client.post()
                        .uri("/jobs")
                        .header("X-User-Id", userId)
                        .contentType(APPLICATION_JSON)
                        .bodyValue(jobInputData)
                        .accept(APPLICATION_JSON)
                        .exchange()
                        .expectStatus().isOk
                        .expectBody<JobDTO>()
                        .consumeWith {
                            val jobOutputData = it.responseBody
                            assertThat(jobOutputData).isNotNull
                            val jobCreatedEvent = consumeLastJobEvent()
                            assertThat(jobCreatedEvent.key()).isEqualTo(jobOutputData!!.id.value)
                            assertThat(jobCreatedEvent.value())
                                    .isInstanceOf(JobPostedEvent::class.java)
                                    .isEqualTo(jobCreatedEvent(jobInputData, jobOutputData.id, userId))
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
                        .expectStatus().isOk
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

        private fun consumeJobEvents(): ConsumerRecords<String, GenericRecord> {
            return KafkaTestUtils.getRecords(jobEventConsumer, Duration.ofSeconds(2).toMillis())
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

        @BeforeAll
        internal fun setUp() {
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

    }


    private fun jobCreatedEvent(jobInputData: JobData, jobId: JobId, userId: String): JobPostedEvent {
        return JobPostedEvent.newBuilder()
                .setId(jobId.value)
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