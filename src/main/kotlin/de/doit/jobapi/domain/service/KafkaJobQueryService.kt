package de.doit.jobapi.domain.service

import de.doit.jobapi.domain.event.JobDataRecord
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
        streamsFactoryBean.kafkaStreams.store(JOB_AGGREGATE_STATE_STORE_NAME, keyValueStore<String, JobDataRecord>())
    }

    companion object {
        fun toJob(jobDataRecord: JobDataRecord): Job {
            return Job(
                    JobId(jobDataRecord.getId()),
                    VendorId(jobDataRecord.getVendorId()),
                    jobDataRecord.getCreatedAt(),
                    jobDataRecord.getModifiedAt(),
                    jobDataRecord.getTitle(),
                    jobDataRecord.getDescription(),
                    jobDataRecord.getLocation().getLatitude(),
                    jobDataRecord.getLocation().getLongitude(),
                    jobDataRecord.getPayment()
            )
        }
    }

    override suspend fun findById(id: JobId): Job? {
        return withContext(Dispatchers.Default) {
            jobLogTableStore.get(id.value)?.let { toJob(it) }
        }
    }

}