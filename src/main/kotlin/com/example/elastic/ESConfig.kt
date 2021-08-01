package com.example.elastic

import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service

@Service
class ESConfig {
    @Bean
    fun es(): RestHighLevelClient {
        return RestHighLevelClient(
            RestClient.builder(
                HttpHost("localhost", 9200, "http")
            )
        )
    }
}