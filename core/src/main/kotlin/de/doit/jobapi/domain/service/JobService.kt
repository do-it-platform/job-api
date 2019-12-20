package de.doit.jobapi.domain.service

import de.doit.jobapi.domain.event.JobDataRecord
import de.doit.jobapi.domain.event.JobDeletedEvent
import de.doit.jobapi.domain.event.JobPostedEvent
import de.doit.jobapi.domain.event.JobUpdatedEvent
import de.doit.jobapi.domain.event.Location
import de.doit.jobapi.domain.exception.JobNotFoundException
import de.doit.jobapi.domain.model.Job
import de.doit.jobapi.domain.model.JobId
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
                    .setLocation(Location.newBuilder()
                            .setLatitude(job.latitude)
                            .setLongitude(job.longitude)
                            .build())
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

    suspend fun update(job: Job): Job {
        return jobQueryService.findById(job.id)
                .let { it ?: throw JobNotFoundException(job.id) }
                .also { if (it.vendorId != job.vendorId) throw IllegalAccessError() }
                .run {
                    val jobUpdatedEvent = JobUpdatedEvent.newBuilder()
                            .setData(toJobDataRecord(job))
                            .build()

                    jobEventPublisher.publish(job.id, jobUpdatedEvent)

                    return job
                }
    }

    suspend fun delete(jobId: JobId, vendorId: VendorId) {
        jobQueryService.findById(jobId)
                .let { it ?: throw JobNotFoundException(jobId) }
                .also { if (it.vendorId != vendorId) throw IllegalAccessError() }
                .run { jobEventPublisher.publish(jobId, JobDeletedEvent()) }
    }

}