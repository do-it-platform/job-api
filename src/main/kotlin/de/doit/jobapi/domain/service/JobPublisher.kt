package de.doit.jobapi.domain.service

import de.doit.jobapi.domain.model.JobId
import org.apache.avro.specific.SpecificRecordBase

interface JobPublisher {
    suspend fun <T: SpecificRecordBase> publish(jobId: JobId, jobEvent: T): T
}