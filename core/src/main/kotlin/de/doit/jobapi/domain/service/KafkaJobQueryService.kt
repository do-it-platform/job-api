package de.doit.jobapi.domain.service

import de.doit.jobapi.domain.event.JobAggregatedEvent
import de.doit.jobapi.domain.model.Job
import de.doit.jobapi.domain.model.JobId
import de.doit.jobapi.domain.model.VendorId
import de.doit.jobapi.domain.service.KafkaStreamsConfig.Companion.JOB_AGGREGATE_STATE_STORE_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.kafka.streams.state.QueryableStoreTypes.keyValueStore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.config.StreamsBuilderFactoryBean
import org.springframework.stereotype.Service


@Service
internal class KafkaJobQueryService(@Autowired private val streamsFactoryBean: StreamsBuilderFactoryBean): JobQueryService {

    private val jobLogTableStore by lazy {
        streamsFactoryBean.kafkaStreams.store(JOB_AGGREGATE_STATE_STORE_NAME, keyValueStore<String, JobAggregatedEvent>())
    }

    companion object {
        fun toJob(jobAggregatedEvent: JobAggregatedEvent): Job {
            return Job(
                    JobId(jobAggregatedEvent.getId()),
                    VendorId(jobAggregatedEvent.getVendorId()),
                    jobAggregatedEvent.getTitle(),
                    jobAggregatedEvent.getDescription(),
                    jobAggregatedEvent.getLatitude(),
                    jobAggregatedEvent.getLongitude(),
                    jobAggregatedEvent.getPayment()
            )
        }
    }

    override suspend fun findById(id: JobId): Job? {
        return withContext(Dispatchers.Default) {
            jobLogTableStore.get(id.value)?.let { toJob(it) }
        }
    }

}