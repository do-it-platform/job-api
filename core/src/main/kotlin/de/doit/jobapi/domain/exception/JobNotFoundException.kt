package de.doit.jobapi.domain.exception

import de.doit.jobapi.domain.model.JobId

class JobNotFoundException(val jobId: JobId): RuntimeException()