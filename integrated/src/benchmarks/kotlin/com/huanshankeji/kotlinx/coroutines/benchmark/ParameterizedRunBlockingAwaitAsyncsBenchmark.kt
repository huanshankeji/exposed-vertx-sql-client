package com.huanshankeji.kotlinx.coroutines.benchmark

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Param
import kotlinx.coroutines.*

class ParameterizedRunBlockingAwaitAsyncsBenchmark : AbstractBenchmark() {
    enum class DispatcherArgumentEnum {
        Default, IO, SingleThread
    }

    @Param
    lateinit var dispatcherArgumentEnum: DispatcherArgumentEnum

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    val singleThreadContext = newSingleThreadContext("single thread")

    @Benchmark
    fun runBlockingAwait1MAsyncsWithDispatcherArgument() =
        runBlocking(
            when (dispatcherArgumentEnum) {
                DispatcherArgumentEnum.Default -> Dispatchers.Default
                DispatcherArgumentEnum.IO -> Dispatchers.IO
                DispatcherArgumentEnum.SingleThread -> singleThreadContext
            }
        ) {
            await1MAasyncs()
        }
}