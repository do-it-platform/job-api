package de.doit.jobapi.domain.service

import de.doit.jobapi.domain.model.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
class JobService(@Autowired private val jobPublisher: JobPublisher) {

    suspend fun add(vendorId: VendorId, job: JobCreationDTO): JobDTO {
        val publishedEvent = jobPublisher.publish(
                JobCreatedEvent.newBuilder()
                        .setId(UUID.randomUUID().toString())
                        .setVendorId(vendorId.value)
                        .setTitle(job.title)
                        .setDescription(job.description)
                        .setLocation(Location.newBuilder().setLatitude(job.latitude).setLongitude(job.longitude).build())
                        .setPayment(job.payment)
                        .build()
        )

        return JobDTO(
                JobId(publishedEvent.getId()),
                publishedEvent.getTitle(),
                publishedEvent.getDescription(),
                publishedEvent.getLocation().getLatitude(),
                publishedEvent.getLocation().getLongitude(),
                publishedEvent.getPayment().toPlainString()
        )
    }

}