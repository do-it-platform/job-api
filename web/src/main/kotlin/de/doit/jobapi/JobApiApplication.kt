package de.doit.jobapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class JobApiApplication

fun main(args: Array<String>) {
	runApplication<JobApiApplication>(*args)
}
