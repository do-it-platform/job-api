package de.doit.jobapi.web

import de.doit.jobapi.web.model.JobRequest
import org.hamcrest.Matchers.anyOf
import org.hamcrest.Matchers.equalTo
import org.jeasy.random.EasyRandom
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EmptySource
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED
import org.springframework.http.MediaType
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@EmbeddedKafka
@DirtiesContext
@ActiveProfiles("test")
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = RANDOM_PORT)
class JobResourceValidationTest(@Autowired private val client: WebTestClient,
                                @Autowired private val easyRandom: EasyRandom) {

    @Nested
    @DisplayName("POST /jobs")
    inner class PostJobs {

        @EmptySource
        @ValueSource(strings = ["    "])
        @DisplayName("with invalid title should return bad request")
        @ParameterizedTest(name = "title = \"{0}\"")
        fun postJobWithMissingTitleShouldReturnBadRequest(title: String) {
            val jobInputData = easyRandom.nextObject(JobRequest::class.java).copy(title = title)

            client.post()
                    .uri("/jobs")
                    .header("X-User-Id", "1234")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(jobInputData)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isBadRequest
        }

        @EmptySource
        @ValueSource(strings = ["    "])
        @DisplayName("with invalid description should return bad request")
        @ParameterizedTest(name = "description = \"{0}\"")
        fun postJobWithMissingDescriptionShouldReturnBadRequest(desc: String) {
            val jobInputData = easyRandom.nextObject(JobRequest::class.java).copy(description = desc)

            client.post()
                    .uri("/jobs")
                    .header("X-User-Id", "1234")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(jobInputData)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isBadRequest
        }

        @ValueSource(doubles = [-3.00])
        @DisplayName("with invalid payment should return bad request")
        @ParameterizedTest(name = "payment = {0}")
        fun postJobWithInvalidPaymentShouldReturnBadRequest(payment: Double) {
            val jobInputData = easyRandom.nextObject(JobRequest::class.java).copy(payment = payment.toBigDecimal())

            client.post()
                    .uri("/jobs")
                    .header("X-User-Id", "1234")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(jobInputData)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isBadRequest
        }

        @ValueSource(doubles = [-91.00, 91.00])
        @DisplayName("with invalid latitude should return bad request")
        @ParameterizedTest(name = "latitude = {0}")
        fun postJobWithInvalidLatitudeShouldReturnBadRequest(latitude: Double) {
            val jobInputData = easyRandom.nextObject(JobRequest::class.java).copy(latitude = latitude)

            client.post()
                    .uri("/jobs")
                    .header("X-User-Id", "1234")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(jobInputData)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isBadRequest
        }

        @ValueSource(doubles = [-181.00, 181.00])
        @DisplayName("with invalid longitude should return bad request")
        @ParameterizedTest(name = "longitude = {0}")
        fun postJobWithInvalidLongitudeShouldReturnBadRequest(longitude: Double) {
            val jobInputData = easyRandom.nextObject(JobRequest::class.java).copy(longitude = longitude)

            client.post()
                    .uri("/jobs")
                    .header("X-User-Id", "1234")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(jobInputData)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isBadRequest
        }

        @NullAndEmptySource
        @ValueSource(strings = ["    "])
        @DisplayName("with invalid X-User-Id header should return bad request")
        @ParameterizedTest(name = "X-User-Id({0})")
        fun postJobWithMissingUserIdShouldReturnBadRequest(userId: String?) {
            client.post()
                    .uri("/jobs")
                    .header("X-User-Id", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(easyRandom.nextObject(JobRequest::class.java))
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isBadRequest
        }

    }

    @Nested
    @DisplayName("PUT /jobs/{id}")
    inner class PutJobs {

        @EmptySource
        @ValueSource(strings = ["    "])
        @DisplayName("with invalid title should return bad request")
        @ParameterizedTest(name = "title = \"{0}\"")
        fun putJobWithMissingTitleShouldReturnBadRequest(title: String) {
            val jobInputData = easyRandom.nextObject(JobRequest::class.java).copy(title = title)

            client.put()
                    .uri("/jobs/{id}", "1234")
                    .header("X-User-Id", "1234")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(jobInputData)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isBadRequest
        }

        @EmptySource
        @ValueSource(strings = ["    "])
        @DisplayName("with invalid description should return bad request")
        @ParameterizedTest(name = "description = \"{0}\"")
        fun putJobWithMissingDescriptionShouldReturnBadRequest(desc: String) {
            val jobInputData = easyRandom.nextObject(JobRequest::class.java).copy(description = desc)

            client.put()
                    .uri("/jobs/{id}", "1234")
                    .header("X-User-Id", "1234")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(jobInputData)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isBadRequest
        }

        @ValueSource(doubles = [-3.00])
        @DisplayName("with invalid payment should return bad request")
        @ParameterizedTest(name = "payment = {0}")
        fun putJobWithInvalidPaymentShouldReturnBadRequest(payment: Double) {
            val jobInputData = easyRandom.nextObject(JobRequest::class.java).copy(payment = payment.toBigDecimal())

            client.put()
                    .uri("/jobs/{id}", "1234")
                    .header("X-User-Id", "1234")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(jobInputData)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isBadRequest
        }

        @ValueSource(doubles = [-91.00, 91.00])
        @DisplayName("with invalid latitude should return bad request")
        @ParameterizedTest(name = "latitude = {0}")
        fun putJobWithInvalidLatitudeShouldReturnBadRequest(latitude: Double) {
            val jobInputData = easyRandom.nextObject(JobRequest::class.java).copy(latitude = latitude)

            client.put()
                    .uri("/jobs/{id}", "1234")
                    .header("X-User-Id", "1234")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(jobInputData)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isBadRequest
        }

        @ValueSource(doubles = [-181.00, 181.00])
        @DisplayName("with invalid longitude should return bad request")
        @ParameterizedTest(name = "longitude = {0}")
        fun putJobWithInvalidLongitudeShouldReturnBadRequest(longitude: Double) {
            val jobInputData = easyRandom.nextObject(JobRequest::class.java).copy(longitude = longitude)

            client.put()
                    .uri("/jobs/{id}", "1234")
                    .header("X-User-Id", "1234")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(jobInputData)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isBadRequest
        }

        @NullAndEmptySource
        @ValueSource(strings = ["    "])
        @DisplayName("with invalid X-User-Id header should return bad request")
        @ParameterizedTest(name = "X-User-Id({0})")
        fun putJobWithMissingUserIdShouldReturnBadRequest(userId: String?) {
            client.put()
                    .uri("/jobs/{id}", "1234")
                    .header("X-User-Id", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(easyRandom.nextObject(JobRequest::class.java))
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isBadRequest
        }

        @NullAndEmptySource
        @ValueSource(strings = ["    "])
        @DisplayName("with invalid JobId should return bad request")
        @ParameterizedTest(name = "JobId({0})")
        fun putJobWithMissingIdShouldReturnBadRequest(id: String?) {
            client.put()
                    .uri("/jobs/{id}", id)
                    .header("X-User-Id", "1234")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(easyRandom.nextObject(JobRequest::class.java))
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().value(anyOf(equalTo(BAD_REQUEST.value()), equalTo(METHOD_NOT_ALLOWED.value())))
        }

    }


    @Nested
    @DisplayName("DELETE /jobs/{id}")
    inner class DeleteJobs {

        @NullAndEmptySource
        @ValueSource(strings = ["    "])
        @DisplayName("with invalid X-User-Id header should return bad request")
        @ParameterizedTest(name = "X-User-Id({0})")
        fun deleteJobWithMissingUserIdShouldReturnBadRequest(userId: String?) {
            client.delete()
                    .uri("/jobs/{id}", "1234")
                    .header("X-User-Id", userId)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isBadRequest
        }

        @NullAndEmptySource
        @ValueSource(strings = ["    "])
        @DisplayName("with invalid JobId should return bad request")
        @ParameterizedTest(name = "JobId({0})")
        fun deleteJobWithMissingIdShouldReturnBadRequest(id: String?) {
            client.delete()
                    .uri("/jobs/{id}", id)
                    .header("X-User-Id", "1234")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().value(anyOf(equalTo(BAD_REQUEST.value()), equalTo(METHOD_NOT_ALLOWED.value())))
        }

    }

}
