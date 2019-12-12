package de.doit.jobapi.domain.service

import de.doit.jobapi.domain.model.JobPostedEvent
import kotlinx.coroutines.future.await
import org.apache.avro.generic.GenericRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class KafkaJobPublisher(@Autowired private val kafkaTemplate: KafkaTemplate<String, GenericRecord>,
                        @Value("\${japi.kafka.topic}") private val topic: String) : JobPublisher {

    override suspend fun publish(jobPostedEvent: JobPostedEvent): JobPostedEvent {
        kafkaTemplate.send(topic, jobPostedEvent.getId(), jobPostedEvent).completable().await()
        return jobPostedEvent
    }
}