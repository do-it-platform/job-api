package de.doit.jobapi.domain.service

import de.doit.jobapi.domain.event.JobAggregatedEvent
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Initializer
import org.apache.kafka.streams.kstream.KTable
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.kstream.ValueTransformer
import org.apache.kafka.streams.kstream.ValueTransformerSupplier
import org.apache.kafka.streams.processor.ProcessorContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafkaStreams

@Configuration
@EnableKafkaStreams
@EnableConfigurationProperties(KafkaConfigProperties::class)
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
    fun jobAggregateStateStore(streamsBuilder: StreamsBuilder): KTable<String, JobAggregatedEvent> {
        return streamsBuilder.stream<String, GenericRecord>(kafkaConfigProperties.topic)
                .transformValues(ValueTransformerSupplier { AddTimestampToRecordTransformer() })
                .groupByKey()
                .aggregate(
                        Initializer<JobAggregatedEvent> { null },
                        JobAggregator(),
                        Materialized.`as`(JOB_AGGREGATE_STATE_STORE_NAME)
                )
    }

    private inner class AddTimestampToRecordTransformer: ValueTransformer<GenericRecord, JobEventWithTimestamp<GenericRecord>> {

        private lateinit var context: ProcessorContext

        override fun init(pctx: ProcessorContext) {
            context = pctx
        }

        override fun transform(value: GenericRecord): JobEventWithTimestamp<GenericRecord> {
            return JobEventWithTimestamp(context.timestamp(), value)
        }

        override fun close() {}

    }

    internal class JobEventWithTimestamp<T: GenericRecord>(val timestamp: Long, val event: T) {

        inline fun <reified T : GenericRecord> specify(): JobEventWithTimestamp<T> {
            return JobEventWithTimestamp<T>(timestamp, event as T)
        }

    }
}