package de.doit.jobapi.domain.service

import de.doit.jobapi.domain.model.JobId
import kotlinx.coroutines.future.await
import org.apache.avro.generic.GenericRecord
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
internal class KafkaJobPublisher(@Autowired private val kafkaTemplate: KafkaTemplate<String, GenericRecord>,
                                 @Value("\${japi.kafka.topic}") private val topic: String) : JobPublisher {

    override suspend fun <T: SpecificRecordBase> publish(jobId: JobId, jobEvent: T): T {
        kafkaTemplate.send(topic, jobId.value, jobEvent).completable().await()
        return jobEvent
    }
}