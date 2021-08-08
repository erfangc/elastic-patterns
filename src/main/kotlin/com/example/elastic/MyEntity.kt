package com.example.elastic

import java.util.*
import kotlin.random.Random.Default.nextInt

data class MyEntity(
    val id: String = UUID.randomUUID().toString(),
    val age: Int = nextInt(),
    val entitlements: Map<String, List<String>> = mapOf(
        "view" to listOf("user:u1", "group:admins", "everyone"),
        "edit" to listOf("group:admins"),
    )
)