package de.doit.jobapi.domain.service

import de.doit.jobapi.domain.event.JobDataRecord
import de.doit.jobapi.domain.event.JobPostedEvent
import de.doit.jobapi.domain.event.JobUpdatedEvent
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.streams.kstream.Aggregator

internal class JobAggregator: Aggregator<String, GenericRecord, JobDataRecord> {

    override fun apply(jobId: String, jobEvent: GenericRecord?, aggregate: JobDataRecord?): JobDataRecord? {
        return when(jobEvent) {
            is JobPostedEvent -> jobEvent.getData()
            is JobUpdatedEvent -> jobEvent.getData()
            else -> null
        }
    }

}