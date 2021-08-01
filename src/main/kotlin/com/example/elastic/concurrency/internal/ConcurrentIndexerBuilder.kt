package com.example.elastic.concurrency.internal

import org.elasticsearch.client.RestHighLevelClient

class ConcurrentIndexerBuilder(private val es: RestHighLevelClient) {

    fun from(index: String, id: String): ConcurrentIndexer {
        return ConcurrentIndexer(
            es = es,
            index = index,
            id = id,
        )
    }

}
