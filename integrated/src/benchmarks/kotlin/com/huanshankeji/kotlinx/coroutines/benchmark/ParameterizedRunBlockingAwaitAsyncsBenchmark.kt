package com.huanshankeji.kotlinx.coroutines.benchmark

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Param
import kotlinx.coroutines.*

class ParameterizedRunBlockingAwaitAsyncsBenchmark : AbstractRunBlockingAwaitAsyncsBenchmark() {
    enum class DispatcherArgumentEnum {
        Default, /*Main,*/ Unconfined, IO, SingleThread
    }

    @Param
    lateinit var dispatcherArgumentEnum: DispatcherArgumentEnum

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    val singleThreadContext = newSingleThreadContext("single thread")
    val dispatcher
        get() = when (dispatcherArgumentEnum) {
            DispatcherArgumentEnum.Default -> Dispatchers.Default
            //DispatcherArgumentEnum.Main -> Dispatchers.Main
            DispatcherArgumentEnum.Unconfined -> Dispatchers.Unconfined
            DispatcherArgumentEnum.IO -> Dispatchers.IO
            DispatcherArgumentEnum.SingleThread -> singleThreadContext
        }


    @Benchmark
    override fun runBlockingAwait1mAsyncs() =
        runBlocking(dispatcher) { await1mAsyncs() }

    @Benchmark
    override fun runBlockingAwait1KAsync1mSums() =
        runBlocking(dispatcher) { await1kAsync1mSums() }
}