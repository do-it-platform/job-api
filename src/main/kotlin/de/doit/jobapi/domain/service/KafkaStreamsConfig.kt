package de.doit.jobapi.domain.service

import de.doit.jobapi.domain.event.JobDataRecord
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Initializer
import org.apache.kafka.streams.kstream.KTable
import org.apache.kafka.streams.kstream.Materialized
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafkaStreams

@Configuration
@EnableKafkaStreams
internal class KafkaStreamsConfig(@Autowired private val kafkaConfigProperties: KafkaConfigProperties) {

    companion object {
        internal const val JOB_AGGREGATE_STATE_STORE_NAME = "job-aggregate-state-store"
    }

    @Bean
    fun createJobLogTopic(): NewTopic {
        return NewTopic(
                kafkaConfigProperties.topic,
                kafkaConfigProperties.numberOfPartitions,
                kafkaConfigProperties.replicationFactor
        )
    }

    @Bean
    fun jobAggregateStateStore(streamsBuilder: StreamsBuilder): KTable<String, JobDataRecord> {
        return streamsBuilder.stream<String, GenericRecord>(kafkaConfigProperties.topic)
                .groupByKey()
                .aggregate(
                        Initializer<JobDataRecord> { null },
                        JobAggregator(),
                        Materialized.`as`(JOB_AGGREGATE_STATE_STORE_NAME)
                )
    }

}