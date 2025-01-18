@file:OptIn(ExperimentalEvscApi::class)

package com.huanshankeji.exposedvertxsqlclient.mysql.vertx.mysqlclient

import com.huanshankeji.Untested
import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient.CoConnectHandler
import com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient.createGenericSqlClient
import com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient.createGenericSqlClientWithBuilder
import com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient.createGenericSqlConnection
import io.vertx.core.Vertx
import io.vertx.mysqlclient.MySQLBuilder
import io.vertx.mysqlclient.MySQLConnectOptions
import io.vertx.mysqlclient.MySQLConnection
import io.vertx.mysqlclient.impl.MySQLPoolOptions
import io.vertx.sqlclient.ClientBuilder
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlClient

/**
 * @see createGenericSqlClient
 */
// made not inline anymore for easier debugging
@ExperimentalEvscApi
fun <SqlClientT : SqlClient, ClientBuilderT : ClientBuilder<SqlClientT>> createGenericPgClientWithBuilder(
    vertx: Vertx?,
    connectionConfig: ConnectionConfig,
    clientBuilder: ClientBuilderT,
    extraPgConnectOptions: MySQLConnectOptions.() -> Unit,
    extraPgPoolOptions: MySQLPoolOptions.() -> Unit,
    connectHandlerExtra: CoConnectHandler
): SqlClientT =
    createGenericSqlClientWithBuilder(
        vertx,
        connectionConfig,
        clientBuilder,
        MySQLConnectOptions(),
        extraPgConnectOptions,
        extraPgPoolOptions,
        connectHandlerExtra,
        MySQLPoolOptions(PoolOptions()) // remain to verify
    )

fun createPgClient(
    vertx: Vertx?,
    connectionConfig: ConnectionConfig,
    extraPgConnectOptions: MySQLConnectOptions.() -> Unit = {},
    extraPoolOptions: MySQLPoolOptions.() -> Unit = {},
    connectHandlerExtra: CoConnectHandler = null,
): SqlClient =
    createGenericPgClientWithBuilder(
        vertx,
        connectionConfig,
        MySQLBuilder.client(),
        extraPgConnectOptions,
        extraPoolOptions,
        connectHandlerExtra
    )

/**
 * @see createGenericSqlClient
 */
fun createPgPool(
    vertx: Vertx?,
    connectionConfig: ConnectionConfig,
    extraPgConnectOptions: MySQLConnectOptions.() -> Unit = {},
    extraPoolOptions: MySQLPoolOptions.() -> Unit = {},
    connectHandlerExtra: CoConnectHandler = null,
): Pool =
    createGenericPgClientWithBuilder(
        vertx,
        connectionConfig,
        MySQLBuilder.pool(),
        extraPgConnectOptions,
        extraPoolOptions,
        connectHandlerExtra
    )

/**
 * @see createGenericSqlClient
 */
@Untested
suspend fun createPgConnection(
    vertx: Vertx?,
    connectionConfig: ConnectionConfig,
    extraPgConnectOptions: MySQLConnectOptions.() -> Unit = {},
    connectHandlerExtra: CoConnectHandler = null
): MySQLConnection =
    createGenericSqlConnection(
        vertx,
        connectionConfig,
        MySQLConnection::connect,
        MySQLConnectOptions(),
        extraPgConnectOptions,
        connectHandlerExtra
    )
