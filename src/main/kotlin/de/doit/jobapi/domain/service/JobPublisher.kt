package de.doit.jobapi.domain.service

import de.doit.jobapi.domain.model.JobCreatedEvent

interface JobPublisher {
    suspend fun publish(jobCreatedEvent: JobCreatedEvent): JobCreatedEvent
}