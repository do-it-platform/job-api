package de.doit.jobapi.web.model

import io.swagger.v3.oas.annotations.media.Schema
import org.hibernate.validator.constraints.Range
import java.math.BigDecimal
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.PositiveOrZero

data class JobRequest(@field:NotBlank
                      @field:Schema(example = "Tutoring")
                      val title: String,
                      @field:NotBlank
                      @field:Schema(example = "Need help with math")
                      val description: String,
                      @field:NotNull
                      @field:Range(min = -90, max = 90)
                      @field:Schema(example = "51.668001")
                      val latitude: Double,
                      @field:NotNull
                      @field:Range(min = -180, max = 180)
                      @field:Schema(example = "-1.0884338")
                      val longitude: Double,
                      @field:NotNull
                      @field:PositiveOrZero
                      @field:Schema(example = "7.99")
                      val payment: BigDecimal)