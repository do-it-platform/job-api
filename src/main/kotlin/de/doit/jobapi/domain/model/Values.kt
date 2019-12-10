package de.doit.jobapi.domain.model

import com.fasterxml.jackson.annotation.JsonValue
import javax.validation.constraints.NotBlank

inline class JobId(@JsonValue val value: String)
inline class VendorId(@field:NotBlank @JsonValue val value: String)