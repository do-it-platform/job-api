package de.doit.jobapi.domain.service

import de.doit.jobapi.domain.model.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
class JobService(@Autowired private val jobPublisher: JobPublisher) {

    suspend fun add(vendorId: VendorId, job: JobData): JobDTO {
        val publishedEvent = jobPublisher.publish(
                JobPostedEvent.newBuilder()
                        .setData(JobDataRecord.newBuilder()
                                .setId(UUID.randomUUID().toString())
                                .setVendorId(vendorId.value)
                                .setTitle(job.title)
                                .setDescription(job.description)
                                .setLocation(Location.newBuilder()
                                        .setLatitude(job.latitude)
                                        .setLongitude(job.longitude)
                                        .build())
                                .setPayment(job.payment)
                                .build()
                        )
                        .build()
        )

        return publishedEvent.getData().let {
            JobDTO(
                    JobId(it.getId()),
                    it.getTitle(),
                    it.getDescription(),
                    it.getLocation().getLatitude(),
                    it.getLocation().getLongitude(),
                    it.getPayment().toPlainString()
            )
        }
    }

}