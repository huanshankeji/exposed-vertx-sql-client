package com.huanshankeji.exposed.benchmark

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlin.concurrent.thread

val numProcessors = Runtime.getRuntime().availableProcessors()

inline fun multiThread_ops_nearlyEvenlyPartitioned_helper(
    numOps: Int,
    numThreads: Int = numProcessors,
    crossinline threadBlock: (num: Int) -> Unit
) {
    // Note that on a device with heterogeneous architecture some threads may finish earlier than others.
    List(numThreads) { i ->
        thread {
            val start = i * numOps / numThreads
            val end = (i + 1) * numOps / numThreads
            threadBlock(end - start)
        }
    }.forEach { it.join() }
}

const val `10K` = 10_000

inline fun multiThread_10K_ops_nearlyEvenlyPartitioned_helper(
    numThreads: Int = numProcessors,
    crossinline threadBlock: (num: Int) -> Unit
) =
    multiThread_ops_nearlyEvenlyPartitioned_helper(`10K`, numThreads, threadBlock)

// decimal instead of 2 ^ 20
const val `1M` = 1_000_000

// ! `await` is actually quite expensive.
@Suppress("SuspendFunctionOnCoroutineScope")
suspend inline fun CoroutineScope.awaitAsync10K(crossinline block: suspend () -> Unit) =
    //List(`10K`) { async { block() } }.awaitAll()
    awaitAll(*Array(`10K`) { async { block() } })
