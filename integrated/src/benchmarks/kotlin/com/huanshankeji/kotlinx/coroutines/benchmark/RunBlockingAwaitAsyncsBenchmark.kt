package com.huanshankeji.kotlinx.coroutines.benchmark

import kotlinx.benchmark.Benchmark
import kotlinx.coroutines.runBlocking

class RunBlockingAwaitAsyncsBenchmark : AbstractRunBlockingAwaitAsyncsBenchmark() {
    @Benchmark
    override fun runBlockingAwait1mAsyncs() =
        runBlocking { await1mAsyncs() }

    @Benchmark
    override fun runBlockingAwait1KAsync1mSums() =
        runBlocking { await1kAsync1mSums() }
}