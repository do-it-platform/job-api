package de.doit.jobapi.domain.model

import java.math.BigDecimal
import java.time.Instant

data class Job(val id: JobId,
               val vendorId: VendorId,
               val createdAt: Instant,
               val modifiedAt: Instant? = null,
               val title: String,
               val description: String,
               val latitude: Double,
               val longitude: Double,
               val payment: BigDecimal)
