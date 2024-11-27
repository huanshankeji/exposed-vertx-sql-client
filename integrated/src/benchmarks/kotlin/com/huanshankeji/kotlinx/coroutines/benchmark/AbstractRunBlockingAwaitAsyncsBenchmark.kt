package com.huanshankeji.kotlinx.coroutines.benchmark

abstract class AbstractRunBlockingAwaitAsyncsBenchmark : AbstractBenchmark() {
    abstract fun runBlockingAwait1mAsyncs()

    abstract fun runBlockingAwait1KAsync1mSums()
}