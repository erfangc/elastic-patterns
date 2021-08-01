package com.example.elastic.concurrency

import java.time.Instant

data class Counter(
    val id: String,
    val count: Long,
    val updatedOn: Instant = Instant.now(),
    val createdOn: Instant = Instant.now(),
)