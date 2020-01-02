package de.doit.jobapi.domain.service

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("jobapi.kafka")
internal data class KafkaConfigProperties(val jobEventSink: TopicConfig, val jobAggregateSink: TopicConfig) {
    data class TopicConfig(val topic: String, val numberOfPartitions: Int, val replicationFactor: Short)
}