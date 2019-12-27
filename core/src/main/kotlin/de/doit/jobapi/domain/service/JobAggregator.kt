package de.doit.jobapi.domain.service

import de.doit.jobapi.domain.event.JobAggregatedEvent
import de.doit.jobapi.domain.event.JobDataRecord
import de.doit.jobapi.domain.event.JobDeletedEvent
import de.doit.jobapi.domain.event.JobPostedEvent
import de.doit.jobapi.domain.event.JobUpdatedEvent
import de.doit.jobapi.domain.service.KafkaStreamsConfig.JobEventWithTimestamp
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.streams.kstream.Aggregator
import java.time.Instant

internal class JobAggregator: Aggregator<String, JobEventWithTimestamp<GenericRecord>, JobAggregatedEvent> {

    companion object {

        private fun create(postedEvent: JobEventWithTimestamp<JobPostedEvent>): JobAggregatedEvent {
            return JobAggregatedEvent
                    .newBuilder()
                    .apply { aggregate(postedEvent.event.getData()) }
                    .apply { this.createdAt = Instant.ofEpochMilli(postedEvent.timestamp) }
                    .build()
        }

        private fun update(jobAggregatedEvent: JobAggregatedEvent,
                           updatedEvent: JobEventWithTimestamp<JobUpdatedEvent>): JobAggregatedEvent {
            return JobAggregatedEvent
                    .newBuilder(jobAggregatedEvent)
                    .apply { aggregate(updatedEvent.event.getData()) }
                    .apply { this.modifiedAt = Instant.ofEpochMilli(updatedEvent.timestamp) }
                    .build()
        }

        private fun JobAggregatedEvent.Builder.aggregate(jobDataRecord: JobDataRecord) {
            this.id = jobDataRecord.getId()
            this.vendorId = jobDataRecord.getVendorId()
            this.title = jobDataRecord.getTitle()
            this.description = jobDataRecord.getDescription()
            this.latitude = jobDataRecord.getLatitude()
            this.longitude = jobDataRecord.getLongitude()
            this.payment = jobDataRecord.getPayment()
        }

    }

    override fun apply(jobId: String,
                       timestampAndEvent: JobEventWithTimestamp<GenericRecord>,
                       aggregate: JobAggregatedEvent?): JobAggregatedEvent? {
        return when(timestampAndEvent.event) {
            is JobPostedEvent -> create(timestampAndEvent.specify())
            is JobUpdatedEvent -> update(aggregate!!, timestampAndEvent.specify())
            is JobDeletedEvent -> null
            else -> aggregate
        }
    }

}