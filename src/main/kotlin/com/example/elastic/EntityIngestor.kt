package com.example.elastic

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.common.xcontent.DeprecationHandler
import org.elasticsearch.common.xcontent.NamedXContentRegistry
import org.elasticsearch.common.xcontent.XContentFactory
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.QueryBuilders.boolQuery
import org.elasticsearch.index.query.QueryBuilders.termsQuery
import org.elasticsearch.index.query.TermsQueryBuilder
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Service
import kotlin.system.exitProcess

//@Service
class EntityIngestor(
    private val restHighLevelClient: RestHighLevelClient
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(EntityIngestor::class.java)
    private val tokens = listOf("user:u1", "everyone")

    private val objectMapper = ObjectMapper()
        .findAndRegisterModules()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    fun entitlementsTermsQuery(): TermsQueryBuilder {
        return termsQuery("entitlements.view.keyword", tokens)
    }

    fun RestHighLevelClient.indexEntity(entity: Any) {
        val indexRequest = IndexRequest(entity::class.simpleName?.lowercase() ?: error("..."))
            .id(entityId(entity))
            .source(objectMapper.writeValueAsString(entity), XContentType.JSON)
        val indexResponse = index(
            indexRequest,
            RequestOptions.DEFAULT
        )
        log.info(indexResponse.toString())
    }

    private fun entityId(entity: Any): String {
        return entity::class
            .members
            .find { it.name == "id" }
            ?.call(entity)
            ?.toString() ?: error("Unable to find a id property on ${entity::class.simpleName}")
    }

    override fun run(vararg args: String?) {
//        restHighLevelClient.indexEntity(MyEntity(entitlements = mapOf("view" to listOf("noone"))))
//        restHighLevelClient.indexEntity(MyEntity())
//        restHighLevelClient.indexEntity(MyEntity())
        val stuff = restHighLevelClient.search(
            SearchRequest(indexName())
                .source(
                    SearchSourceBuilder
                        .searchSource()
                        .query(
                            boolQuery()
                                .filter(entitlementsTermsQuery())
                        )
                ),
            RequestOptions.DEFAULT
        )

        val hits = stuff.hits.hits
        hits.forEach { log.info(it.sourceAsString) }
        exitProcess(0)
    }

    private fun indexName() = MyEntity::class.simpleName?.lowercase() ?: error("...")
}

fun main() {
    val builder = XContentFactory.jsonBuilder().prettyPrint()
    val json = """
{
  "took" : 5,
  "timed_out" : false,
  "_shards" : {
    "total" : 1,
    "successful" : 1,
    "skipped" : 0,
    "failed" : 0
  },
  "hits" : {
    "total" : {
      "value" : 1,
      "relation" : "eq"
    },
    "max_score" : 0.0,
    "hits" : [
      {
        "_index" : "myentity",
        "_type" : "_doc",
        "_id" : "1c2ceeb3-080f-4a98-ac71-03f75407de75",
        "_score" : 0.0,
        "_source" : {
          "id" : "1c2ceeb3-080f-4a98-ac71-03f75407de75",
          "age" : 2053822794,
          "entitlements" : {
            "view" : [
              "noone"
            ]
          }
        }
      }
    ]
  }
}
    """.trimIndent()
    val xContentParser = XContentFactory
        .xContent(XContentType.JSON)
        .createParser(
            NamedXContentRegistry.EMPTY,
            DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
            json.toByteArray()
        )

    val objectMapper = ObjectMapper().findAndRegisterModules().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    val searchResponse = SearchResponse.fromXContent(xContentParser)
    val searchHits = searchResponse.hits
    println(searchHits.totalHits)
    val hits = searchHits.hits
    hits.forEach { hit ->
        println(hit)
        val entity = objectMapper.readValue(hit.sourceAsString, MyEntity::class.java)
        println(entity)
    }
    println(searchResponse)
}