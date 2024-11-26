package com.huanshankeji.exposed.benchmark.coroutine

import com.huanshankeji.exposed.benchmark.AbstractBenchmark
import com.huanshankeji.exposed.benchmark.TransactionBenchmark
import kotlinx.benchmark.Benchmark
import kotlinx.coroutines.runBlocking

/**
 * As a comparison for [TransactionBenchmark].
 */
class RunBlockingAwaitAsyncsBenchmark : AbstractBenchmark() {
    @Benchmark
    fun runBlockingAwait1MAsyncs() =
        runBlocking { await1MAasyncs() }
}