package de.doit.jobapi.domain.model

import java.math.BigDecimal
import javax.validation.constraints.NotBlank
import javax.validation.constraints.PositiveOrZero

data class JobCreationDTO(@field:NotBlank val title: String,
                          @field:NotBlank val description: String,
                          val latitude: Double,
                          val longitude: Double,
                          @field:PositiveOrZero val payment: BigDecimal)
