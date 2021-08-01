# Elasticsearch Concurrency Demo

In this example code, we show how to ensure that re-Indexing operations do not
accidentally overwrite updates that occurred in-between the time the client code started processing and 
the time the client code attempted to commit an update

