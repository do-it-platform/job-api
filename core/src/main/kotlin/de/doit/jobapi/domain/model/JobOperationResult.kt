package de.doit.jobapi.domain.model

sealed class JobOperationResult {
    object Success : JobOperationResult()
    object NotFound : JobOperationResult()
    object Forbidden : JobOperationResult()
}