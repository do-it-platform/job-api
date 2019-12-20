package de.doit.jobapi.web.model

import de.doit.jobapi.domain.model.JobId
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Job")
data class JobResponse(val id: JobId,
                       val title: String,
                       val description: String,
                       val latitude: Double,
                       val longitude: Double,
                       val payment: String)