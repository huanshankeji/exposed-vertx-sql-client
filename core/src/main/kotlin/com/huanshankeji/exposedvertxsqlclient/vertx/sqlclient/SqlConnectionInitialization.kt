package com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient

import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import io.vertx.core.Handler
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.SqlConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Extra initialization on [SqlConnection] in addition to [setRole] for [ConnectionConfig.UnixDomainSocketWithPeerAuthentication].
 */
typealias ConnectHandlerExtra = (suspend (SqlConnection) -> Unit)?
typealias ExtensionConnectHandlerExtra = (suspend SqlConnection.() -> Unit)?

fun ExtensionConnectHandlerExtra.toWithParameterFunction(): ConnectHandlerExtra =
    this

suspend fun ConnectionConfig.initConnection(sqlConnection: SqlConnection, extra: ConnectHandlerExtra) {
    when (this) {
        is ConnectionConfig.Socket -> Unit // do nothing
        is ConnectionConfig.UnixDomainSocketWithPeerAuthentication -> sqlConnection.setRole(role)
    }
    extra?.let { it(sqlConnection) }
}

fun ConnectionConfig.getConnectHandler(extra: ConnectHandlerExtra): Handler<SqlConnection>? =
    when (this) {
        is ConnectionConfig.Socket -> extra?.let {
            // TODO extract a common `coConnectHandler`
            Handler {
                CoroutineScope(Dispatchers.Unconfined).launch {
                    it(it)
                    it.close().coAwait()
                }
            }
        }

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
