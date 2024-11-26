package com.huanshankeji.kotlinx.coroutines.benchmark

import kotlinx.benchmark.Benchmark
import kotlinx.coroutines.runBlocking

class RunBlockingAwaitAsyncsBenchmark : AbstractBenchmark() {
    @Benchmark
    fun runBlockingAwait1MAsyncs() =
        runBlocking { await1MAasyncs() }
}