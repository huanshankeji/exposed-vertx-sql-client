@file:OptIn(ExperimentalEvscApi::class)

package com.huanshankeji.exposedvertxsqlclient.mssql.vertx.mssqlclient

import com.huanshankeji.Untested
import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient.CoConnectHandler
import com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient.createGenericSqlClient
import com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient.createGenericSqlClientWithBuilder
import com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient.createGenericSqlConnection
import io.vertx.core.Vertx
import io.vertx.mssqlclient.MSSQLBuilder
import io.vertx.mssqlclient.MSSQLConnectOptions
import io.vertx.mssqlclient.MSSQLConnection
import io.vertx.sqlclient.ClientBuilder
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlClient

/**
 * @see createGenericSqlClient
 */
// made not inline anymore for easier debugging
@ExperimentalEvscApi
fun <SqlClientT : SqlClient, ClientBuilderT : ClientBuilder<SqlClientT>> createGenericMssqlClientWithBuilder(
    vertx: Vertx?,
    connectionConfig: ConnectionConfig,
    clientBuilder: ClientBuilderT,
    extraMssqlConnectOptions: MSSQLConnectOptions.() -> Unit,
    extraMssqlPoolOptions: PoolOptions.() -> Unit,
    connectHandlerExtra: CoConnectHandler
): SqlClientT =
    createGenericSqlClientWithBuilder(
        vertx,
        connectionConfig,
        clientBuilder,
        MSSQLConnectOptions(),
        extraMssqlConnectOptions,
        extraMssqlPoolOptions,
        connectHandlerExtra,
        PoolOptions()
    )

/**
 * @see createGenericSqlClient
 */
fun createMssqlPool(
    vertx: Vertx?,
    connectionConfig: ConnectionConfig,
    extraMssqlConnectOptions: MSSQLConnectOptions.() -> Unit = {},
    extraPoolOptions: PoolOptions.() -> Unit = {},
    connectHandlerExtra: CoConnectHandler = null,
): Pool =
    createGenericMssqlClientWithBuilder(
        vertx,
        connectionConfig,
        MSSQLBuilder.pool(),
        extraMssqlConnectOptions,
        extraPoolOptions,
        connectHandlerExtra
    )

/**
 * @see createGenericSqlClient
 * @param vertx Non-null. See [MSSQLConnection.connect].
 */
@Untested
suspend fun createMssqlConnection(
    vertx: Vertx,
    connectionConfig: ConnectionConfig,
    extraMssqlConnectOptions: MSSQLConnectOptions.() -> Unit = {},
    connectHandlerExtra: CoConnectHandler = null
): MSSQLConnection =
    createGenericSqlConnection(
        vertx,
        connectionConfig,
        MSSQLConnection::connect,
        MSSQLConnectOptions(),
        extraMssqlConnectOptions,
        connectHandlerExtra
    )
