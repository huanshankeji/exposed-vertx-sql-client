package com.huanshankeji.exposed.benchmark

import kotlin.concurrent.thread

val numProcessors = Runtime.getRuntime().availableProcessors()

inline fun multiThread_operations_nearlyEvenlyPartitioned_helper(
    numOperations: Int,
    numThreads: Int = numProcessors,
    crossinline threadBlock: (num: Int) -> Unit
) {
    // Note that on a device with heterogeneous architecture some threads may finish earlier than others.
    List(numThreads) { i ->
        thread {
            val start = i * numOperations / numThreads
            val end = (i + 1) * numOperations / numThreads
            threadBlock(end - start)
        }
    }.forEach { it.join() }
}

const val `10K` = 10_000

inline fun multiThread_10K_nearlyEvenlyPartitioned_helper(
    numThreads: Int = numProcessors,
    crossinline threadBlock: (num: Int) -> Unit
) =
    multiThread_operations_nearlyEvenlyPartitioned_helper(`10K`, numThreads, threadBlock)
