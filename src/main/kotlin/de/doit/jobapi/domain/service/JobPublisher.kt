package de.doit.jobapi.domain.service

import de.doit.jobapi.domain.model.JobPostedEvent

interface JobPublisher {
    suspend fun publish(jobPostedEvent: JobPostedEvent): JobPostedEvent
}