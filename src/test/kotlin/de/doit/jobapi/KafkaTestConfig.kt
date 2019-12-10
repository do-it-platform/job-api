package de.doit.jobapi

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*


@Configuration
class KafkaTestConfig(private val props: KafkaProperties) {

    @Bean
    fun schemaRegistryClient(): SchemaRegistryClient = MockSchemaRegistryClient()

    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, GenericRecord> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, GenericRecord>()
        factory.consumerFactory = consumerFactory()
        return factory
    }

    @Bean
    fun consumerFactory(): ConsumerFactory<String, GenericRecord> {
        val consumerProps = props.buildConsumerProperties()
        consumerProps[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG]
        return DefaultKafkaConsumerFactory(
                consumerProps,
                StringDeserializer(),
                genericAvroSerde().deserializer()
        )
    }

    @Bean
    fun genericAvroSerde(): GenericAvroSerde {
        val genericAvroSerde = GenericAvroSerde(schemaRegistryClient())
        genericAvroSerde.configure(mapOf(
                KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS to true,
                KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "http://mock:8081",
                KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to true,
                KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "http://mock:8081"
        ), false)
        return genericAvroSerde
    }

    @Bean
    fun producerFactory(): ProducerFactory<String, GenericRecord> {
        val producerProps = props.buildProducerProperties()
        producerProps[ProducerConfig.MAX_BLOCK_MS_CONFIG] = 3000
        return DefaultKafkaProducerFactory(
                producerProps,
                StringSerializer(),
                genericAvroSerde().serializer()
        )
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, GenericRecord> {
        return KafkaTemplate<String, GenericRecord>(producerFactory())
    }

}