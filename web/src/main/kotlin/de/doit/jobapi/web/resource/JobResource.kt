package de.doit.jobapi.web.resource

import de.doit.jobapi.domain.model.JobId
import de.doit.jobapi.domain.model.JobOperationResult.Forbidden
import de.doit.jobapi.domain.model.JobOperationResult.NotFound
import de.doit.jobapi.domain.model.JobOperationResult.Success
import de.doit.jobapi.domain.model.VendorId
import de.doit.jobapi.domain.service.JobService
import de.doit.jobapi.web.mapping.JobMapper
import de.doit.jobapi.web.model.JobRequest
import de.doit.jobapi.web.model.JobResponse
import kotlinx.coroutines.reactor.mono
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import java.util.UUID
import javax.validation.ConstraintViolationException
import javax.validation.Valid
import javax.validation.constraints.NotBlank


@Validated
@RestController
@RequestMapping("/jobs")
class JobResource(@Autowired private val jobMapper: JobMapper, @Autowired private val jobService: JobService) {

    @ResponseStatus(CREATED)
    @PostMapping(consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    fun add(@RequestHeader("X-User-Id") @NotBlank vendorId: VendorId,
            @RequestBody @Valid data: JobRequest): Mono<JobResponse> {
        return mono {
            val job = jobMapper.mapToDomainModel(
                    jobId = JobId(UUID.randomUUID().toString()),
                    vendorId = vendorId,
                    jobRequest = data
            )
            jobMapper.mapToDTO(jobService.add(job))
        }
    }

    @PutMapping("/{id}", consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    fun update(@PathVariable @NotBlank id: JobId,
               @RequestHeader("X-User-Id") @NotBlank vendorId: VendorId,
               @RequestBody @Valid data: JobRequest): Mono<ResponseEntity<Void>> {
        // for some reason Mono<Unit> return empty object -> {} instead empty response body
        return mono { jobService.update(jobMapper.mapToDomainModel(id, vendorId, data)) }
                .map {
                    when (it) {
                        is Success -> ResponseEntity.noContent().build<Void>()
                        is NotFound -> ResponseEntity.status(NOT_FOUND).build()
                        is Forbidden -> ResponseEntity.status(FORBIDDEN).build()
                    }
                }
    }

    @DeleteMapping("/{id}", produces = [APPLICATION_JSON_VALUE])
    fun delete(@PathVariable @NotBlank id: JobId,
               @RequestHeader("X-User-Id") @NotBlank vendorId: VendorId): Mono<ResponseEntity<Void>> {
        return mono { jobService.delete(id, vendorId) }
                .map {
                    when (it) {
                        is Success -> ResponseEntity.noContent().build<Void>()
                        is NotFound -> ResponseEntity.status(NOT_FOUND).build()
                        is Forbidden -> ResponseEntity.status(FORBIDDEN).build()
                    }
                }
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(e: ConstraintViolationException) {
        throw ResponseStatusException(BAD_REQUEST, e.message, e)
    }

}