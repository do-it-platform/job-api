package de.doit.jobapi.domain.service

import de.doit.jobapi.domain.model.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
class JobService internal constructor(@Autowired private val jobPublisher: JobPublisher) {

    companion object {

        private fun toJobDataRecord(jobId: JobId, vendorId: VendorId, job: JobData): JobDataRecord {
            return JobDataRecord.newBuilder()
                    .setId(jobId.value)
                    .setVendorId(vendorId.value)
                    .setTitle(job.title)
                    .setDescription(job.description)
                    .setLocation(Location.newBuilder()
                            .setLatitude(job.latitude)
                            .setLongitude(job.longitude)
                            .build())
                    .setPayment(job.payment)
                    .build()
        }

        private fun toDTO(jobDataRecord: JobDataRecord): JobDTO {
            return JobDTO(
                    JobId(jobDataRecord.getId()),
                    jobDataRecord.getTitle(),
                    jobDataRecord.getDescription(),
                    jobDataRecord.getLocation().getLatitude(),
                    jobDataRecord.getLocation().getLongitude(),
                    jobDataRecord.getPayment().toPlainString()
            )
        }

    }

    suspend fun add(vendorId: VendorId, job: JobData): JobDTO {
        val jobId = JobId(UUID.randomUUID().toString())
        val jobPostedEvent = JobPostedEvent.newBuilder()
                .setData(toJobDataRecord(jobId, vendorId, job))
                .build()
        jobPublisher.publish(jobId, jobPostedEvent)

        return toDTO(jobPostedEvent.getData())
    }

    suspend fun update(jobId: JobId, vendorId: VendorId, job: JobData) {
        val jobUpdatedEvent = JobUpdatedEvent.newBuilder()
                .setData(toJobDataRecord(jobId, vendorId, job))
                .build()
        jobPublisher.publish(jobId, jobUpdatedEvent)
    }

}