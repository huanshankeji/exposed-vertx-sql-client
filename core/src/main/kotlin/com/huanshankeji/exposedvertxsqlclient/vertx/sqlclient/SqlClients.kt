package com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient

import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.vertx.sqlclient.setUpConventionally
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.*

/**
 * Exposed generates prepared statements and [SqlConnectOptions.cachePreparedStatements] improves performance greatly (tested on PostgreSQL)
 * so it's enabled by default.
 */
@ExperimentalEvscApi
// made inline for possible suspend calls
// TODO consider removing the default arguments so we don't forget to pass them in this library's functions
inline fun <SqlClientT : SqlClient, SqlConnectOptionsT : SqlConnectOptions, PoolOptionsT : PoolOptions?> createGenericSqlClient(
    vertx: Vertx?,
    connectionConfig: ConnectionConfig,
    sqlConnectOptionsFromConstructor: SqlConnectOptionsT,
    extraSqlConnectOptions: SqlConnectOptionsT.() -> Unit = {},
    poolOptionsFromConstructor: PoolOptionsT,
    extraPoolOptions: PoolOptionsT.() -> Unit = {},
    noinline connectHandlerExtra: ConnectHandlerExtra = null,
    create: (Vertx?, SqlConnectOptionsT, PoolOptionsT, ConnectHandlerExtra) -> SqlClientT
): SqlClientT {
    val sqlConnectOptions = sqlConnectOptionsFromConstructor.apply {
        setUpConventionally()
        setFrom(connectionConfig)
        extraSqlConnectOptions()
    }

    return create(vertx, sqlConnectOptions, poolOptionsFromConstructor.apply(extraPoolOptions), connectHandlerExtra)
}

/**
 * @see createGenericSqlClient
 */
@ExperimentalEvscApi
// made not inline anymore for easier debugging
fun <SqlClientT : SqlClient, SqlConnectOptionsT : SqlConnectOptions, PoolOptionsT : PoolOptions?, ClientBuilderT : ClientBuilder<SqlClientT>> createGenericSqlClientWithBuilder(
    vertx: Vertx?,
    connectionConfig: ConnectionConfig,
    clientBuilder: ClientBuilderT,
    sqlConnectOptionsFromConstructor: SqlConnectOptionsT,
    extraSqlConnectOptions: SqlConnectOptionsT.() -> Unit = {},
    extraPoolOptions: PoolOptionsT.() -> Unit = {},
    connectHandlerExtra: ConnectHandlerExtra = null,
    poolOptionsFromConstructor: PoolOptionsT
): SqlClientT =
    @Suppress("NAME_SHADOWING")
    createGenericSqlClient(
        vertx,
        connectionConfig,
        sqlConnectOptionsFromConstructor,
        extraSqlConnectOptions,
        poolOptionsFromConstructor,
        extraPoolOptions,
        connectHandlerExtra
    ) { vertx, database, options, connectHandlerExtra ->
        clientBuilder.apply {
            using(vertx)
            connectingTo(database)
            with(options)
            val connectHandler = connectionConfig.getConnectHandler(connectHandlerExtra)
            connectHandler?.let { withConnectHandler(it) }
        }.build()
    }

/**
 * @see createGenericSqlClient
 */
@ExperimentalEvscApi
// made not inline anymore for easier debugging
suspend fun <SqlConnectionT : SqlConnection, SqlConnectOptionsT : SqlConnectOptions> createGenericSqlConnection(
    vertx: Vertx?,
    connectionConfig: ConnectionConfig,
    sqlConnectionConnect: (Vertx?, SqlConnectOptionsT) -> Future<SqlConnectionT>,
    sqlConnectOptionsFromConstructor: SqlConnectOptionsT,
    extraSqlConnectOptions: SqlConnectOptionsT.() -> Unit = {},
    connectHandlerExtra: ConnectHandlerExtra = null
): SqlConnectionT =
    @Suppress("NAME_SHADOWING")
    createGenericSqlClient(
        vertx,
        connectionConfig,
        sqlConnectOptionsFromConstructor,
        extraSqlConnectOptions,
        null,
        {},
        connectHandlerExtra
    ) { vertx, database, _, connectHandlerExtra ->
        sqlConnectionConnect(vertx, database).coAwait().also {
            connectionConfig.initConnection(it, connectHandlerExtra)
        }
    }
