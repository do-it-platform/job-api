package de.doit.jobapi.domain.service

import de.doit.jobapi.domain.model.*
import de.doit.jobapi.domain.service.KafkaStreamsConfig.Companion.JOB_LOG_TABLE_STORE_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.streams.state.QueryableStoreTypes.keyValueStore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.config.StreamsBuilderFactoryBean
import org.springframework.stereotype.Service


@Service
internal class KafkaJobQueryService(@Autowired private val streamsFactoryBean: StreamsBuilderFactoryBean): JobQueryService {

    private val jobLogTableStore by lazy {
        streamsFactoryBean.kafkaStreams.store(JOB_LOG_TABLE_STORE_NAME, keyValueStore<String, GenericRecord>())
    }

    companion object {
        fun toJob(jobDataRecord: JobDataRecord): Job {
            return Job(
                    JobId(jobDataRecord.getId()),
                    VendorId(jobDataRecord.getVendorId()),
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
            jobLogTableStore.get(id.value)?.let {
                println(it::class)
                println(it.javaClass)
                println(it is JobPostedEvent)
                println(JobPostedEvent::class.isInstance(it))
                when (it) {
                    is JobPostedEvent -> toJob(it.getData())
                    is JobUpdatedEvent -> toJob(it.getData())
                    else -> null
                }
            }
        }
    }

}