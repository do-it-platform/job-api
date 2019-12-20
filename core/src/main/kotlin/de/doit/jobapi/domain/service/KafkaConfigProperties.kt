package de.doit.jobapi.domain.service

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("japi.kafka")
internal data class KafkaConfigProperties(val topic: String, val numberOfPartitions: Int, val replicationFactor: Short)