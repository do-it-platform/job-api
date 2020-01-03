package de.doit.jobapi.domain.service

import de.doit.jobapi.domain.event.JobDataRecord
import de.doit.jobapi.domain.event.JobDeletedEvent
import de.doit.jobapi.domain.event.JobPostedEvent
import de.doit.jobapi.domain.event.JobUpdatedEvent
import de.doit.jobapi.domain.model.Job
import de.doit.jobapi.domain.model.JobId
import de.doit.jobapi.domain.model.JobOperationResult
import de.doit.jobapi.domain.model.JobOperationResult.Forbidden
import de.doit.jobapi.domain.model.JobOperationResult.NotFound
import de.doit.jobapi.domain.model.JobOperationResult.Success
import de.doit.jobapi.domain.model.VendorId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class JobService internal constructor(@Autowired private val jobEventPublisher: JobEventPublisher,
                                      @Autowired private val jobQueryService: JobQueryService) {

    companion object {

        private fun toJobDataRecord(job: Job): JobDataRecord {
            return JobDataRecord.newBuilder()
                    .setId(job.id.value)
                    .setVendorId(job.vendorId.value)
                    .setTitle(job.title)
                    .setDescription(job.description)
                    .setLatitude(job.latitude)
                    .setLongitude(job.longitude)
                    .setPayment(job.payment)
                    .build()
        }

    }

    suspend fun add(job: Job): Job {
        val jobPostedEvent = JobPostedEvent.newBuilder()
                .setData(toJobDataRecord(job))
                .build()

        jobEventPublisher.publish(job.id, jobPostedEvent)

        return job
    }

    suspend fun update(job: Job): JobOperationResult {
        return jobQueryService.findById(job.id)
                .let { it ?: return NotFound }
                .also { if (it.vendorId != job.vendorId) return Forbidden }
                .run {
                    val jobUpdatedEvent = JobUpdatedEvent.newBuilder()
                            .setData(toJobDataRecord(job))
                            .build()

                    jobEventPublisher.publish(job.id, jobUpdatedEvent)

                    return Success
                }
    }

    suspend fun delete(jobId: JobId, vendorId: VendorId): JobOperationResult {
        return jobQueryService.findById(jobId)
                .let { it ?: return NotFound }
                .also { if (it.vendorId != vendorId) return Forbidden }
                .run { jobEventPublisher.publish(jobId, JobDeletedEvent()) }
                .let { Success }
    }

}