package de.doit.jobapi.domain.service

import de.doit.jobapi.domain.model.Job
import de.doit.jobapi.domain.model.JobId

internal interface JobQueryService {
    suspend fun findById(id: JobId): Job?
}