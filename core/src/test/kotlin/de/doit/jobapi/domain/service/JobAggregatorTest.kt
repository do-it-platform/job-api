package de.doit.jobapi.domain.service

import com.github.javafaker.Faker
import de.doit.jobapi.domain.event.JobDeletedEvent
import de.doit.jobapi.domain.event.JobPostedEvent
import de.doit.jobapi.domain.event.JobUpdatedEvent
import de.doit.jobapi.domain.service.KafkaStreamsConfig.JobEventWithTimestamp
import org.apache.avro.generic.GenericRecord
import org.assertj.core.api.Assertions.assertThat
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.jeasy.random.FieldPredicates
import org.jeasy.random.api.Randomizer
import org.junit.jupiter.api.Test
import java.time.Instant

internal class JobAggregatorTest {

    private val jobAggregator: JobAggregator = JobAggregator()
    private val easyRandom: EasyRandom = EasyRandom(EasyRandomParameters()
            .randomize(FieldPredicates.named("payment"), Randomizer { Faker().commerce().price().toBigDecimal() })
    )

    @Test
    internal fun testJobPostedAggregation() {
        val jobPostedEventTimestamp = Instant.now().toEpochMilli()
        val jobPostedEvent = easyRandom.nextObject(JobPostedEvent::class.java)
        val timestampAndEvent = JobEventWithTimestamp<GenericRecord>(jobPostedEventTimestamp, jobPostedEvent)

        val jobAggregatedEvent = jobAggregator.apply("1234", timestampAndEvent, null)

        assertThat(jobAggregatedEvent).isNotNull()

        with(jobAggregatedEvent!!) {
            assertThat(this.getId()).isEqualTo(jobPostedEvent.getData().getId())
            assertThat(this.getVendorId()).isEqualTo(jobPostedEvent.getData().getVendorId())
            assertThat(this.getTitle()).isEqualTo(jobPostedEvent.getData().getTitle())
            assertThat(this.getDescription()).isEqualTo(jobPostedEvent.getData().getDescription())
            assertThat(this.getLatitude()).isEqualTo(jobPostedEvent.getData().getLatitude())
            assertThat(this.getLongitude()).isEqualTo(jobPostedEvent.getData().getLongitude())
            assertThat(this.getPayment()).isEqualTo(jobPostedEvent.getData().getPayment())
            assertThat(this.getCreatedAt().toEpochMilli()).isEqualTo(jobPostedEventTimestamp)
        }
    }

    @Test
    internal fun testJobUpdatedAggregation() {
        val jobPostedEventTimestamp = Instant.now().toEpochMilli()
        val jobPostedEvent = easyRandom.nextObject(JobPostedEvent::class.java)
        val timestampAndJobPostedEvent = JobEventWithTimestamp<GenericRecord>(jobPostedEventTimestamp, jobPostedEvent)

        val jobUpdatedEventTimestamp = Instant.now().toEpochMilli()
        val jobUpdatedEvent = easyRandom.nextObject(JobUpdatedEvent::class.java)
        val timestampAndJobUpdatedEvent = JobEventWithTimestamp<GenericRecord>(jobUpdatedEventTimestamp, jobUpdatedEvent)

        val jobPostedAggregate = jobAggregator.apply("1234", timestampAndJobPostedEvent, null)
        val jobUpdateAggregate = jobAggregator.apply("1234", timestampAndJobUpdatedEvent, jobPostedAggregate)

        assertThat(jobUpdateAggregate).isNotNull()

        with(jobUpdateAggregate!!) {
            assertThat(this.getId()).isEqualTo(jobUpdatedEvent.getData().getId())
            assertThat(this.getVendorId()).isEqualTo(jobUpdatedEvent.getData().getVendorId())
            assertThat(this.getTitle()).isEqualTo(jobUpdatedEvent.getData().getTitle())
            assertThat(this.getDescription()).isEqualTo(jobUpdatedEvent.getData().getDescription())
            assertThat(this.getLatitude()).isEqualTo(jobUpdatedEvent.getData().getLatitude())
            assertThat(this.getLongitude()).isEqualTo(jobUpdatedEvent.getData().getLongitude())
            assertThat(this.getPayment()).isEqualTo(jobUpdatedEvent.getData().getPayment())
            assertThat(this.getCreatedAt().toEpochMilli()).isEqualTo(jobPostedEventTimestamp)
            assertThat(this.getModifiedAt().toEpochMilli()).isEqualTo(jobUpdatedEventTimestamp)
        }
    }

    @Test
    internal fun testJobDeletedAggregation() {
        val jobPostedEventTimestamp = Instant.now().toEpochMilli()
        val jobPostedEvent = easyRandom.nextObject(JobPostedEvent::class.java)
        val timestampAndJobPostedEvent = JobEventWithTimestamp<GenericRecord>(jobPostedEventTimestamp, jobPostedEvent)

        val jobDeletedEventTimestamp = Instant.now().toEpochMilli()
        val jobDeletedEvent = easyRandom.nextObject(JobDeletedEvent::class.java)
        val timestampAndJobDeletedEvent = JobEventWithTimestamp<GenericRecord>(jobDeletedEventTimestamp, jobDeletedEvent)

        val jobPostedAggregate = jobAggregator.apply("1234", timestampAndJobPostedEvent, null)
        val jobDeletedAggregate = jobAggregator.apply("1234", timestampAndJobDeletedEvent, jobPostedAggregate)

        assertThat(jobDeletedAggregate).isNull()
    }

}