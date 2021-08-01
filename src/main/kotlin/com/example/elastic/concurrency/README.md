# Elasticsearch Concurrency Demo

In this example code, we show how to ensure that re-Indexing operations do not accidentally overwrite updates that
occurred in-between the time the client code started processing and the time the client code attempted to commit an
update

## Problem

Let's say you are keeping an Elasticsearch index up-to-date with its source data. Further, assume your ES index is the
amalgamation of different upstream tables denormalized into a single searchable collection. For example:
index `orders` contains documents representing `order` objects joined on `product` and `user`

Assuming updates from `order`, `product` and `user` tables are streamed into an indexing pipeline independently - then
you run the risk of race conditions occurring. For example, during the time it takes to merge an update in `user` another
parallel processor could've processed an update for `product`

## Example usage of this library
```kotlin
indexerBuilder
    .from(index = "counter", id = id)
    .update(maxAttempts = 10) { source ->
        val counter = objectMapper.readValue<Counter>(source)
        objectMapper.writeValueAsString(counter.copy(count = counter.count + 1))
    }
```

## How does it work?

> See [Elasticsearch Optimistic Concurrent Control](https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-index_.html#optimistic-concurrency-control-index)

Whereas ordinary updates on Elasticsearch passes in a single value,
the `update()` function accepts a function called `next`, which has the signature `(current: String) -> String`

The `next` function can be invoked repeatedly to build the most correct up-to-date snapshot if a previous attempt
to update the index fails

When an update fails due to a conflict on the server (i.e. someone else modified the document in-between the time
you started to process your change and when you attempted to save) the library will encounter a `CONFLICT` HTTP status code.
Internally the library will invoke `next` again with the most recent copy of the document to produce the next attempt
