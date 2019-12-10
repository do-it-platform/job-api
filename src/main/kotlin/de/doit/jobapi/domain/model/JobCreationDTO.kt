package de.doit.jobapi.domain.model

import io.swagger.v3.oas.annotations.media.Schema
import org.hibernate.validator.constraints.Range
import java.math.BigDecimal
import javax.validation.constraints.NotBlank
import javax.validation.constraints.PositiveOrZero

@Schema(name = "JobData")
data class JobCreationDTO(@field:NotBlank val title: String,
                          @field:NotBlank val description: String,
                          @field:Range(min = -90, max = 90) val latitude: Double,
                          @field:Range(min = -180, max = 180) val longitude: Double,
                          @field:PositiveOrZero val payment: BigDecimal)
