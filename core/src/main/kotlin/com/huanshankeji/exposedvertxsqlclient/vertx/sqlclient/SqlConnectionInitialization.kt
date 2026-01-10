@file:OptIn(ExperimentalUnixDomainSocketApi::class, ExperimentalApi::class)

package com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient

import com.huanshankeji.ExperimentalApi
import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalUnixDomainSocketApi
import io.vertx.sqlclient.SqlConnection

/**
 * Extra initialization on [SqlConnection] in addition to [setRole] for [ConnectionConfig.UnixDomainSocketWithPeerAuthentication].
 */
typealias CoConnectHandler = (suspend (SqlConnection) -> Unit)?
typealias ExtensionCoConnectHandler = (suspend SqlConnection.() -> Unit)?

fun ExtensionCoConnectHandler.toWithParameterFunction(): CoConnectHandler =
    this

suspend fun ConnectionConfig.initConnection(sqlConnection: SqlConnection, extra: CoConnectHandler) {
    when (this) {
        is ConnectionConfig.Socket -> Unit // do nothing
        is ConnectionConfig.UnixDomainSocketWithPeerAuthentication -> sqlConnection.setRole(role)
    }
    extra?.let { it(sqlConnection) }
}

fun ConnectionConfig.getCoConnectHandler(extra: CoConnectHandler): CoConnectHandler =
    when (this) {
        is ConnectionConfig.Socket -> extra
        is ConnectionConfig.UnixDomainSocketWithPeerAuthentication -> {
            {
                it.setRole(role)
                extra?.let { extra -> extra(it) }
            }
        }
    }
