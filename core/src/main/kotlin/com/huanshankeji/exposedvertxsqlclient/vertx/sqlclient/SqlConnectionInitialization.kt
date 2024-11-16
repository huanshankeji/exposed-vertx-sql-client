package com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient

import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import io.vertx.core.Handler
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.SqlConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

suspend fun ConnectionConfig.initConnection(sqlConnection: SqlConnection) {
    when (this) {
        is ConnectionConfig.Socket -> Unit // do nothing
        is ConnectionConfig.UnixDomainSocketWithPeerAuthentication -> sqlConnection.setRole(role)
    }
}

// TODO make `extra` `suspend`
fun ConnectionConfig.connectHandler(extra: ((SqlConnection) -> Unit)?): Handler<SqlConnection>? =
    when (this) {
        is ConnectionConfig.Socket -> extra?.let { Handler { it(it) } }
        is ConnectionConfig.UnixDomainSocketWithPeerAuthentication -> Handler {
            CoroutineScope(Dispatchers.Unconfined).launch {
                // TODO: are exceptions handled?
                it.setRole(role)
                extra?.let { extra -> extra(it) }
                /** @see Pool.connectHandler */
                it.close().coAwait()
            }
        }
    }
