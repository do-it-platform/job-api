package de.doit.jobapi.web

import de.doit.jobapi.domain.model.JobCreationDTO
import de.doit.jobapi.domain.model.JobDTO
import de.doit.jobapi.domain.model.VendorId
import de.doit.jobapi.domain.service.JobService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.NotBlank

@RestController
@RequestMapping("/jobs", consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
class JobResource(@Autowired private val jobService: JobService) {

    @PostMapping
    suspend fun add(@RequestHeader("X-User-Id") @Valid @NotBlank vendorId: String,
                    @RequestBody @Valid  data: JobCreationDTO): JobDTO {
        return jobService.add(VendorId(vendorId), data)
    }

}