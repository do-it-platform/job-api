package de.doit.jobapi.web

import de.doit.jobapi.domain.model.JobCreationDTO
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
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

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
            val jobInputData = easyRandom.nextObject(JobCreationDTO::class.java).copy(title = title)

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
            val jobInputData = easyRandom.nextObject(JobCreationDTO::class.java).copy(description = desc)

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
            val jobInputData = easyRandom.nextObject(JobCreationDTO::class.java).copy(payment = payment.toBigDecimal())

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
            val jobInputData = easyRandom.nextObject(JobCreationDTO::class.java).copy(latitude = latitude)

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
            val jobInputData = easyRandom.nextObject(JobCreationDTO::class.java).copy(longitude = longitude)

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
                    .bodyValue(easyRandom.nextObject(JobCreationDTO::class.java))
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isBadRequest
        }

    }

}
