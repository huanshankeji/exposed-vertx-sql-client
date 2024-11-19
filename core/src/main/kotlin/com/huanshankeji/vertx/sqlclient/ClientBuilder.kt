// TODO move to "kotlin-common"

package com.huanshankeji.vertx.sqlclient

import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.ClientBuilder
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.SqlConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A coroutine variant of [ClientBuilder.withConnectHandler].
 * You don't need to call [SqlConnection.close] in [handler].
 */
fun ClientBuilder<*>.withCoConnectHandler(handler: suspend (SqlConnection) -> Unit) =
    withConnectHandler {
        CoroutineScope(Dispatchers.Unconfined).launch {
            // TODO What happens when there are exceptions in `handler`? Are they correctly propagated?
            handler(it)
            /** @see Pool.connectHandler */
            it.close().coAwait()
        }
    }
