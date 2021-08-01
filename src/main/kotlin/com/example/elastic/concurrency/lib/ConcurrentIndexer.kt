package com.example.elastic.concurrency.lib

import org.elasticsearch.ElasticsearchException
import org.elasticsearch.action.DocWriteResponse
import org.elasticsearch.action.get.GetRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.rest.RestStatus
import org.slf4j.LoggerFactory

class ConcurrentIndexer(
    private val es: RestHighLevelClient,
    private val index: String,
    private val id: String,
) {

    private val log = LoggerFactory.getLogger(ConcurrentIndexer::class.java)

    fun update(maxAttempts: Int, next: (current: String) -> String) {
        updateWithRetryInternal(
            attempt = 1,
            maxAttempts = maxAttempts,
            next = next,
        )
    }

    /**
     * Attempt to update a given document using a callback function. The parameter to the callback function
     * is the existing copy of this document as retrieved from the database
     *
     * This function will use Optimistic Locking to prevent overwrite of more recent data. The function provided here
     * i.e. [next] will be invoked repeatedly if writes are dirty
     *
     * @param attempt the current attempt in the recursion
     * @param maxAttempts maximum attempts to try before giving up
     * @param next the function that produces the next version of the object we wish to update
     */
    private fun updateWithRetryInternal(
        attempt: Int,
        maxAttempts: Int,
        next: (currentVersion: String) -> String
    ) {
        /*
        get the currentVersion
         */
        val get = es.get(GetRequest(index).id(id), RequestOptions.DEFAULT)
        val seqNo = get.seqNo
        val primaryTerm = get.primaryTerm

        /*
        Attempt to index
         */
        try {
            log.info("Updating document /$index/$id attempt=$attempt")
            val nextSource = next.invoke(get.sourceAsString)
            val indexResponse = es.index(
                IndexRequest(index)
                    .id(id)
                    .source(nextSource, XContentType.JSON)
                    .setIfSeqNo(seqNo)
                    .setIfPrimaryTerm(primaryTerm),
                RequestOptions.DEFAULT
            )

            if (indexResponse.result == DocWriteResponse.Result.UPDATED) {
                log.info("Updated document /$index/$id attempt=$attempt")
            } else {
                throw RuntimeException("Unable to update document $id in index $index result=${indexResponse.result}")
            }

        } catch (ex: ElasticsearchException) {
            if (ex.status() == RestStatus.CONFLICT) {
                if (attempt >= maxAttempts) {
                    throw RuntimeException("Unable to update document $id in index $index after $attempt attempts")
                } else {
                    log.info("Conflict detected while updating id=$id index=$index on attempt=$attempt")
                    // recurse
                    updateWithRetryInternal(
                        attempt = attempt + 1,
                        maxAttempts = maxAttempts,
                        next = next,
                    )
                }
            }
        }
    }

}