package de.doit.jobapi.domain.service

import de.doit.jobapi.domain.model.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

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

        private fun createJob(jobId: JobId, vendorId: VendorId, jobData: JobData): Job {
            return Job(
                    jobId,
                    vendorId,
                    jobData.title,
                    jobData.description,
                    jobData.latitude,
                    jobData.longitude,
                    jobData.payment
            )
        }

    }

    suspend fun add(vendorId: VendorId, jobData: JobData): Job {
        val jobId = JobId(UUID.randomUUID().toString())
        val job = createJob(jobId, vendorId, jobData)

        val jobPostedEvent = JobPostedEvent.newBuilder()
                .setData(toJobDataRecord(job))
                .build()

        jobEventPublisher.publish(jobId, jobPostedEvent)

        return job
    }

    suspend fun update(jobId: JobId, vendorId: VendorId, jobData: JobData): Job? {
        return jobQueryService.findById(jobId)
                ?.also { if (it.vendorId != vendorId) throw IllegalAccessError() }
                ?.run {
                    val updatedJob = createJob(jobId, vendorId, jobData)

                    val jobUpdatedEvent = JobUpdatedEvent.newBuilder()
                            .setData(toJobDataRecord(updatedJob))
                            .build()

                    jobEventPublisher.publish(jobId, jobUpdatedEvent)

                    return updatedJob
                }
    }

}