package de.doit.jobapi.web

import de.doit.jobapi.domain.model.JobDTO
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class JobResource {

    @GetMapping("/jobs", produces = [APPLICATION_JSON_VALUE])
    fun add(): Mono<JobDTO> {
        return Mono.just(JobDTO("foo"))
    }

}