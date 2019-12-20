package de.doit.jobapi.web.mapping

import de.doit.jobapi.domain.model.Job
import de.doit.jobapi.domain.model.JobId
import de.doit.jobapi.domain.model.VendorId
import de.doit.jobapi.web.model.JobRequest
import de.doit.jobapi.web.model.JobResponse
import org.mapstruct.Mapper

//TODO replace once mapstruct support immutable data classes
@Mapper
abstract class JobMapper {

    fun mapToDomainModel(jobId: JobId, vendorId: VendorId, jobRequest: JobRequest): Job {
        return Job(
                id = jobId,
                vendorId = vendorId,
                title = jobRequest.title,
                description = jobRequest.description,
                latitude = jobRequest.latitude,
                longitude = jobRequest.longitude,
                payment = jobRequest.payment
        )
    }

    fun mapToDTO(job: Job): JobResponse {
        return JobResponse(
                id = job.id,
                title = job.title,
                description = job.description,
                latitude = job.latitude,
                longitude = job.longitude,
                payment = job.payment.toPlainString()
        )
    }

}