package de.doit.jobapi

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

@TestConfiguration
class SchemaRegistryTestConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    fun schemaRegistryContainer(kafkaBroker: EmbeddedKafkaBroker): GenericContainer<*> {
        return GenericContainer<Nothing>("confluentinc/cp-schema-registry")
                .apply {
                    withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8091")
                    withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
                    withEnv("SCHEMA_REGISTRY_KAFKASTORE_CONNECTION_URL", kafkaBroker.zookeeperConnectionString)
                    withNetworkMode("host")
                    waitingFor(Wait.forLogMessage(".*Server started, listening for requests.*", 1))
                }
    }

}