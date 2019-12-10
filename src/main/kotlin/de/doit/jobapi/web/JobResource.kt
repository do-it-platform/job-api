package de.doit.jobapi.web

import de.doit.jobapi.domain.model.JobCreationDTO
import de.doit.jobapi.domain.model.JobDTO
import de.doit.jobapi.domain.model.VendorId
import de.doit.jobapi.domain.service.JobService
import kotlinx.coroutines.reactor.mono
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import javax.validation.ConstraintViolationException
import javax.validation.Valid
import javax.validation.constraints.NotBlank


@Validated
@RestController
@RequestMapping("/jobs")
class JobResource(@Autowired private val jobService: JobService) {

    @Valid
    @PostMapping(consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    fun add(@RequestHeader("X-User-Id") @NotBlank vendorId: VendorId,
            @RequestBody @Valid data: JobCreationDTO): Mono<JobDTO> {
        return mono { jobService.add(vendorId, data) }
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(e: ConstraintViolationException) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message, e)
    }

}