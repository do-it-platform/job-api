package de.doit.jobapi.web

import de.doit.jobapi.domain.model.JobCreationDTO
import de.doit.jobapi.domain.model.JobDTO
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import javax.validation.Valid

@Validated
@RestController
class JobResource {

    @PostMapping("/jobs", produces = [APPLICATION_JSON_VALUE])
    fun add(@Valid @RequestBody data: JobCreationDTO): Mono<JobDTO> {
        return Mono.empty()
    }

}