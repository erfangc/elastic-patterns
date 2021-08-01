package com.example.elastic.concurrency.example

import com.example.elastic.concurrency.example.Utils.idUnderTest
import com.example.elastic.concurrency.example.Utils.objectMapper
import com.example.elastic.concurrency.internal.ConcurrentIndexerBuilder
import org.elasticsearch.action.get.GetRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.common.xcontent.XContentType
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
class ExampleCommandLineRunner(private val es: RestHighLevelClient) : CommandLineRunner {

    private val executor = Executors.newFixedThreadPool(2)
    private val log = LoggerFactory.getLogger(ExampleCommandLineRunner::class.java)

    override fun run(vararg args: String?) {
        val indexerBuilder = ConcurrentIndexerBuilder(es)

        val getResponse = es.get(GetRequest("counter").id(idUnderTest), RequestOptions.DEFAULT)
        if (!getResponse.isExists) {
            log.info("Creating document $idUnderTest")
            es.index(
                IndexRequest("counter").id(idUnderTest)
                    .source(objectMapper.writeValueAsString(Counter(id = idUnderTest, count = 0)), XContentType.JSON),
                RequestOptions.DEFAULT,
            )
        } else {
            log.info("Document $idUnderTest already exists")
        }

        executor.submit {
            CounterUpdater(indexerBuilder = indexerBuilder).run()
        }
        executor.submit {
            CounterUpdater(indexerBuilder = indexerBuilder).run()
        }
    }
}