package de.doit.jobapi.domain.model

import com.fasterxml.jackson.annotation.JsonValue

inline class JobId(@JsonValue val value: String)
inline class VendorId(@JsonValue val value: String)