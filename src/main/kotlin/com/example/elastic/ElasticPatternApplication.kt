package com.example.elastic

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ElasticPatternApplication

fun main(args: Array<String>) {
	runApplication<ElasticPatternApplication>(*args)
}
