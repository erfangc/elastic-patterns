package com.example.elastic.concurrency.example

import com.example.elastic.concurrency.example.Utils.idUnderTest
import com.example.elastic.concurrency.example.Utils.objectMapper
import com.example.elastic.concurrency.internal.ConcurrentIndexerBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import kotlin.random.Random

class CounterUpdater(private val indexerBuilder: ConcurrentIndexerBuilder) {

    private val log = LoggerFactory.getLogger(CounterUpdater::class.java)

    fun run() {
        try {
            while (true) {
                /*
                these are the params
                 */
                val id = idUnderTest

                indexerBuilder
                    .from(index = "counter", id = id)
                    .update(maxAttempts = 10) { source ->
                        sleepRandomly()
                        val counter = objectMapper.readValue<Counter>(source)
                        objectMapper.writeValueAsString(counter.copy(count = counter.count + 1))
                    }
                sleepRandomly()
            }
        } catch (ex: Exception) {
            log.error("Error", ex)
        }
    }

    private fun sleepRandomly() {
        Thread.sleep(
            Random(System.currentTimeMillis()).nextLong(100, 2000)
        )
    }

}