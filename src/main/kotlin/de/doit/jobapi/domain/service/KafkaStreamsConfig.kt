package de.doit.jobapi.domain.service

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KTable
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.state.KeyValueStore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafkaStreams

@Configuration
@EnableKafkaStreams
internal class KafkaStreamsConfig(@Autowired private val kafkaConfigProperties: KafkaConfigProperties,
                                  @Autowired private val kafkaProperties: KafkaProperties) {

    companion object {
        internal const val JOB_LOG_TABLE_STORE_NAME = "job-log-table"
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
    fun avroSerde(): GenericAvroSerde {
        val genericAvroSerde = GenericAvroSerde()
        genericAvroSerde.configure(mapOf(
                SCHEMA_REGISTRY_URL_CONFIG to kafkaProperties.properties[SCHEMA_REGISTRY_URL_CONFIG],
                VALUE_SUBJECT_NAME_STRATEGY to kafkaProperties.properties[VALUE_SUBJECT_NAME_STRATEGY],
                SPECIFIC_AVRO_READER_CONFIG to true
        ), false)

        return genericAvroSerde
    }

    @Bean
    fun jobLogTable(streamsBuilder: StreamsBuilder, avroSerde: GenericAvroSerde): KTable<String, GenericRecord> {

        return streamsBuilder.table(
                kafkaConfigProperties.topic,
                Consumed.with(Serdes.String(), avroSerde),
                Materialized.`as`<String, GenericRecord, KeyValueStore<Bytes, ByteArray>>(JOB_LOG_TABLE_STORE_NAME)
        )
    }

}