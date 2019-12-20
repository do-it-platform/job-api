package de.doit.jobapi.web

import de.doit.jobapi.domain.exception.JobNotFoundException
import de.doit.jobapi.domain.model.*
import de.doit.jobapi.domain.service.JobService
import kotlinx.coroutines.reactor.mono
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.*
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
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

    @ResponseStatus(CREATED)
    @PostMapping(consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    fun add(@RequestHeader("X-User-Id") @NotBlank vendorId: VendorId,
            @RequestBody @Valid data: JobData): Mono<JobDTO> {
        return mono { toDTO(jobService.add(vendorId, data)) }
    }

    @PutMapping("/{id}", consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    fun update(@PathVariable @NotBlank id: JobId,
               @RequestHeader("X-User-Id") @NotBlank vendorId: VendorId,
               @RequestBody @Valid data: JobData): Mono<ResponseEntity<Void>> {
        // for some reason Mono<Unit> return empty object -> {} instead empty response body
        return mono { jobService.update(id, vendorId, data) }
                .map { ResponseEntity.noContent().build<Void>() }
                .onErrorReturn(JobNotFoundException::class.java, ResponseEntity.status(NOT_FOUND).build())
                .onErrorReturn(IllegalAccessError::class.java, ResponseEntity.status(FORBIDDEN).build())
    }

    @DeleteMapping("/{id}", produces = [APPLICATION_JSON_VALUE])
    fun delete(@PathVariable @NotBlank id: JobId,
               @RequestHeader("X-User-Id") @NotBlank vendorId: VendorId): Mono<ResponseEntity<Void>> {
        return mono { jobService.delete(id, vendorId) }
                .map { ResponseEntity.noContent().build<Void>() }
                .onErrorReturn(JobNotFoundException::class.java, ResponseEntity.status(NOT_FOUND).build())
                .onErrorReturn(IllegalAccessError::class.java, ResponseEntity.status(FORBIDDEN).build())
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(e: ConstraintViolationException) {
        throw ResponseStatusException(BAD_REQUEST, e.message, e)
    }

    companion object {

        private fun toDTO(job: Job): JobDTO {
            return JobDTO(
                    job.id,
                    job.title,
                    job.description,
                    job.latitude,
                    job.longitude,
                    job.payment.toPlainString()
            )
        }

    }

}