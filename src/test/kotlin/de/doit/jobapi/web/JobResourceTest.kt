package de.doit.jobapi.web

import de.doit.jobapi.domain.model.*
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.serialization.StringDeserializer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ObjectAssert
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.math.BigDecimal
import java.time.Duration
import java.util.*
import kotlin.reflect.jvm.javaField

@AutoConfigureWebClient
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@EmbeddedKafka(topics = ["\${japi.kafka.topic}"])
class JobResourceTest {

    @Autowired lateinit var client: WebTestClient
    @Autowired lateinit var kafkaBroker: EmbeddedKafkaBroker
    @Value("\${japi.kafka.topic}") private lateinit var topic: String

    @Nested
    @DisplayName("POST /jobs")
    inner class PostJob {

        @Test
        @DisplayName("should publish job-created event")
        fun postJobShouldPublishJobCreatedEvent() {
            val userId = "2342-56456"
            val jobInputData = EasyRandom().nextObject(JobCreationDTO::class.java)

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
                                .isInstanceOf(JobCreatedEvent::class.java)
                                .isEqualTo(jobCreatedEvent(jobInputData, jobOutputData.id, userId))
                    }
        }

        @Test
        @DisplayName("should return dto after success")
        fun postJobShouldReturnDtoAfterSuccessfullyPublishedEvent() {
            val jobInputData = EasyRandom().nextObject(JobCreationDTO::class.java)

            client.post()
                    .uri("/jobs")
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
        }

        @NullAndEmptySource
        @ValueSource(strings = ["    "])
        @DisplayName("should return bad request")
        @ParameterizedTest(name = "with title({0})")
        fun postJobWithMissingTitleShouldReturnBadRequest(title: String?) {
            val parameters = EasyRandomParameters().randomize({ f -> f == JobCreationDTO::title.javaField }, { title })
            val jobInputData = EasyRandom(parameters).nextObject(JobCreationDTO::class.java)

            client.post()
                    .uri("/jobs")
                    .contentType(APPLICATION_JSON)
                    .bodyValue(jobInputData)
                    .accept(APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isBadRequest

            assertThat(consumeJobEvents()).isEmpty()
        }

        @NullAndEmptySource
        @ValueSource(strings = ["    "])
        @DisplayName("should return bad request")
        @ParameterizedTest(name = "with description({0})")
        fun postJobWithMissingDescriptionShouldReturnBadRequest(desc: String?) {
            val parameters = EasyRandomParameters().randomize({ f -> f == JobCreationDTO::description.javaField }, { desc })
            val jobInputData = EasyRandom(parameters).nextObject(JobCreationDTO::class.java)

            client.post()
                    .uri("/jobs")
                    .contentType(APPLICATION_JSON)
                    .bodyValue(jobInputData)
                    .accept(APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isBadRequest

            assertThat(consumeJobEvents()).isEmpty()
        }

        @NullSource
        @ValueSource(doubles = [-3.00])
        @DisplayName("should return bad request")
        @ParameterizedTest(name = "with payment({0})")
        fun postJobWithInvalidPaymentShouldReturnBadRequest(payment: Double?) {
            val parameters = EasyRandomParameters().randomize(
                    { f -> f == JobCreationDTO::payment.javaField },
                    { payment?.let { BigDecimal.valueOf(payment) } }
            )
            val jobInputData = EasyRandom(parameters).nextObject(JobCreationDTO::class.java)

            client.post()
                    .uri("/jobs")
                    .contentType(APPLICATION_JSON)
                    .bodyValue(jobInputData)
                    .accept(APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isBadRequest

            assertThat(consumeJobEvents()).isEmpty()
        }

        @Nested
        @DisplayName("without kafka available")
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        inner class JobResourceTestWithoutKafkaAvailable {

            @BeforeAll
            internal fun beforeAll() {
                kafkaBroker.kafkaServers.forEach {
                    it.shutdown()
                    it.awaitShutdown()
                }
            }

            @Test
            @DisplayName("should return server error")
            fun postJobShouldReturnErrorWhenKafkaIsNotAvailable() {
                val jobInputData = EasyRandom().nextObject(JobCreationDTO::class.java)

                client.post()
                        .uri("/jobs")
                        .contentType(APPLICATION_JSON)
                        .bodyValue(jobInputData)
                        .accept(APPLICATION_JSON)
                        .exchange()
                        .expectStatus().is5xxServerError
            }

        }

    }


    private fun consumeJobEvents(): ConsumerRecords<String, GenericRecord> {
        val consumer = avroConsumer()
        kafkaBroker.consumeFromAnEmbeddedTopic(consumer, topic)
        return KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5).toMillis())
    }

    private fun consumeLastJobEvent(): ConsumerRecord<String, GenericRecord> {
        val records = consumeJobEvents()
        assertThat(records).hasSize(1)
        return records.first()
    }

    private fun avroConsumer(): Consumer<String, GenericRecord> {
        val consumerProps = KafkaTestUtils.consumerProps("test-group-${UUID.randomUUID()}", "true", kafkaBroker)
        consumerProps[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        val genericAvroSerde = GenericAvroSerde(MockSchemaRegistryClient())
        val cf: ConsumerFactory<String, GenericRecord> = DefaultKafkaConsumerFactory(
                consumerProps,
                StringDeserializer(),
                genericAvroSerde.deserializer()
        )
        return cf.createConsumer()
    }

    private fun jobCreatedEvent(jobInputData: JobCreationDTO, jobId: JobId, userId: String): JobCreatedEvent {
        return JobCreatedEvent.newBuilder()
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

    private fun ObjectAssert<JobDTO>.containsValuesOf(jobCreationDTO: JobCreationDTO) {
        satisfies {
            it.apply {
                assertThat(title).isEqualTo(jobCreationDTO.title)
                assertThat(description).isEqualTo(jobCreationDTO.description)
                assertThat(latitude).isEqualTo(jobCreationDTO.latitude)
                assertThat(longitude).isEqualTo(jobCreationDTO.longitude)
                assertThat(payment).isEqualTo(jobCreationDTO.payment)
            }
        }
    }

}