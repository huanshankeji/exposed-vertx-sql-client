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
fun <SqlClientT : SqlClient, ClientBuilderT : ClientBuilder<SqlClientT>> createGenericMysqlClientWithBuilder(
    vertx: Vertx?,
    connectionConfig: ConnectionConfig,
    clientBuilder: ClientBuilderT,
    extraMysqlConnectOptions: MySQLConnectOptions.() -> Unit,
    extraMysqlPoolOptions: MySQLPoolOptions.() -> Unit,
    connectHandlerExtra: CoConnectHandler
): SqlClientT =
    createGenericSqlClientWithBuilder(
        vertx,
        connectionConfig,
        clientBuilder,
        MySQLConnectOptions(),
        extraMysqlConnectOptions,
        extraMysqlPoolOptions,
        connectHandlerExtra,
        MySQLPoolOptions(PoolOptions()) // remain to verify
    )

fun createMysqlClient(
    vertx: Vertx?,
    connectionConfig: ConnectionConfig,
    extraMysqlConnectOptions: MySQLConnectOptions.() -> Unit = {},
    extraPoolOptions: MySQLPoolOptions.() -> Unit = {},
    connectHandlerExtra: CoConnectHandler = null,
): SqlClient =
    createGenericMysqlClientWithBuilder(
        vertx,
        connectionConfig,
        MySQLBuilder.client(),
        extraMysqlConnectOptions,
        extraPoolOptions,
        connectHandlerExtra
    )

/**
 * @see createGenericSqlClient
 */
fun createMysqlPool(
    vertx: Vertx?,
    connectionConfig: ConnectionConfig,
    extraMysqlConnectOptions: MySQLConnectOptions.() -> Unit = {},
    extraPoolOptions: MySQLPoolOptions.() -> Unit = {},
    connectHandlerExtra: CoConnectHandler = null,
): Pool =
    createGenericMysqlClientWithBuilder(
        vertx,
        connectionConfig,
        MySQLBuilder.pool(),
        extraMysqlConnectOptions,
        extraPoolOptions,
        connectHandlerExtra
    )

/**
 * @see createGenericSqlClient
 */
@Untested
suspend fun createMysqlConnection(
    vertx: Vertx?,
    connectionConfig: ConnectionConfig,
    extraMysqlConnectOptions: MySQLConnectOptions.() -> Unit = {},
    connectHandlerExtra: CoConnectHandler = null
): MySQLConnection =
    createGenericSqlConnection(
        vertx,
        connectionConfig,
        MySQLConnection::connect,
        MySQLConnectOptions(),
        extraMysqlConnectOptions,
        connectHandlerExtra
    )
