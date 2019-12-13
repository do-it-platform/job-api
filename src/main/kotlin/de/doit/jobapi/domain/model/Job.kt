package de.doit.jobapi.domain.model

import java.math.BigDecimal

data class Job(val id: JobId,
               val vendorId: VendorId,
               val title: String,
               val description: String,
               val latitude: Double,
               val longitude: Double,
               val payment: BigDecimal)
