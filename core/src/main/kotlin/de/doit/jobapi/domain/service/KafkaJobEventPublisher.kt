package de.doit.jobapi.domain.service

import de.doit.jobapi.domain.model.JobId
import kotlinx.coroutines.future.await
import org.apache.avro.generic.GenericRecord
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
internal class KafkaJobEventPublisher(@Autowired private val kafkaTemplate: KafkaTemplate<String, GenericRecord>,
                                      @Autowired private val kafkaConfigProperties: KafkaConfigProperties) : JobEventPublisher {

    override suspend fun <T: SpecificRecordBase> publish(jobId: JobId, jobEvent: T): T {
        val (jobEventSink, _) = kafkaConfigProperties
        kafkaTemplate.send(jobEventSink.topic, jobId.value, jobEvent).completable().await()
        return jobEvent
    }
}