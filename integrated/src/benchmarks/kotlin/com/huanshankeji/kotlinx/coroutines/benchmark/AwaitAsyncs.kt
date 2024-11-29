package com.huanshankeji.kotlinx.coroutines.benchmark

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

@Suppress("SuspendFunctionOnCoroutineScope")
internal suspend inline fun CoroutineScope.await1mAsyncs() {
    List(1 shl 20) { async {} }.awaitAll()
}

internal val `1mSizeList` = List(1 shl 20) { it }

// It seems the loop get optimized and removed in this function.
@Suppress("SuspendFunctionOnCoroutineScope")
internal suspend inline fun CoroutineScope.await1kAsync1mLoops() {
    List(1 shl 10) { async { repeat(1 shl 20) {} } }.awaitAll()
}

@Suppress("SuspendFunctionOnCoroutineScope")
internal suspend inline fun CoroutineScope.await1kAsync1mSums() {
    List(1 shl 10) { async { `1mSizeList`.sum() } }.awaitAll()
}
