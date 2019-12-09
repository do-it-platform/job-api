package de.doit.jobapi.domain.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Job")
data class JobDTO(val id: JobId,
                  val title: String,
                  val description: String,
                  val latitude: Double,
                  val longitude: Double,
                  val payment: String)