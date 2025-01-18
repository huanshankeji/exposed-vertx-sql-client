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
import io.vertx.sqlclient.ClientBuilder
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.SqlClient
import io.vertx.mysqlclient.MySQLBuilder
import io.vertx.mysqlclient.MySQLConnection
// import io.vertx.mysqlclient.MySQLPool --deprecated
import io.vertx.mysqlclient.MySQLConnectOptions
import io.vertx.mysqlclient.impl.MySQLPoolOptions
import io.vertx.sqlclient.PoolOptions


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

// temporarily added, maybe moved into kotlin-common
public fun io.vertx.mysqlclient.impl.MySQLPoolOptions.setUpConventionally(): kotlin.Unit { /* compiled code */ }

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
