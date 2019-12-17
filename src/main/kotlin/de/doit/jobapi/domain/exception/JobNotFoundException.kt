package de.doit.jobapi.domain.exception

import de.doit.jobapi.domain.model.JobId
import java.lang.RuntimeException

class JobNotFoundException(val jobId: JobId): RuntimeException() {}