package com.huanshankeji.exposedvertxsqlclient.postgresql.vertx.pgclient

import com.huanshankeji.Untested
import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient.connectHandler
import com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient.createGenericSqlClient
import com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient.initConnection
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.pgclient.PgBuilder
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgConnection
import io.vertx.pgclient.impl.PgPoolOptions
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.SqlConnection

inline fun <SqlClientT : SqlClient, PgPoolOptionsT : PgPoolOptions?> createGenericPgClient(
    vertx: Vertx?,
    connectionConfig: ConnectionConfig,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {},
    pgPoolOptionsFromConstructor: PgPoolOptionsT,
    extraPgPoolOptions: PgPoolOptionsT.() -> Unit = {},
    create: (Vertx?, PgConnectOptions, PgPoolOptionsT) -> SqlClientT
): SqlClientT =
    createGenericSqlClient(
        vertx,
        connectionConfig,
        PgConnectOptions(),
        extraPgConnectOptions,
        pgPoolOptionsFromConstructor,
        extraPgPoolOptions,
        create
    )

fun createPgClient(
    vertx: Vertx?,
    connectionConfig: ConnectionConfig,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {},
    extraPoolOptions: PgPoolOptions.() -> Unit = {},
    connectHandlerExtra: ((SqlConnection) -> Unit)? = null, // TODO add to `createGenericSqlClient`
): SqlClient =
    createGenericPgClient(
        vertx,
        connectionConfig,
        extraPgConnectOptions,
        PgPoolOptions(),
        extraPoolOptions
    ) { vertx, database, options ->
        PgBuilder.client()
            .apply {
                using(vertx)
                connectingTo(database)
                with(options)
                // TODO move to an overload of `createGenericSqlClient`
                val connectHandler = connectionConfig.connectHandler(connectHandlerExtra)
                connectHandler?.let { withConnectHandler(it) }
            }
            .build()
    }

/**
 * [PgPoolOptions.pipelined] is enabled by default.
 */
fun createPgPool(
    vertx: Vertx?,
    connectionConfig: ConnectionConfig,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {},
    extraPoolOptions: PgPoolOptions.() -> Unit = {},
    connectHandlerExtra: ((SqlConnection) -> Unit)? = null, // TODO add to `createGenericSqlClient`
): Pool =
    createGenericPgClient(
        vertx,
        connectionConfig,
        extraPgConnectOptions,
        PgPoolOptions(),
        {
            // TODO consider extracting this into a conventional config and move to "kotlin-common", decoupling it from this library
            isPipelined = true
            extraPoolOptions()
        }
    ) { vertx, database, options ->
        PgBuilder.pool()
            .apply {
                using(vertx)
                connectingTo(database)
                with(options)
                // TODO move to an overload of `createGenericSqlClient`
                val connectHandler = connectionConfig.connectHandler(connectHandlerExtra)
                connectHandler?.let { withConnectHandler(it) }
            }
            .build()
    }

@Untested
suspend fun createPgConnection(
    vertx: Vertx?,
    connectionConfig: ConnectionConfig.Socket,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}
): PgConnection =
    createGenericPgClient(
        vertx, connectionConfig, extraPgConnectOptions, null
    ) { vertx, pgConnectOptions, _ ->
        PgConnection.connect(vertx, pgConnectOptions).coAwait().also {
            connectionConfig.initConnection(it)
        }
    }