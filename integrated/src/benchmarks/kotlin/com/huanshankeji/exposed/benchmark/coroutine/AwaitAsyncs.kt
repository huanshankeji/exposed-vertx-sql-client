package com.huanshankeji.exposed.benchmark.coroutine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

@Suppress("SuspendFunctionOnCoroutineScope")
internal suspend inline fun CoroutineScope.await1MAasyncs() {
    List(1 shl 20) { async {} }.awaitAll()
}
