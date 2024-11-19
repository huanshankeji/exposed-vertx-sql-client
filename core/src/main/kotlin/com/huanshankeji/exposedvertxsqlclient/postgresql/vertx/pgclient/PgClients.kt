@file:OptIn(ExperimentalEvscApi::class)

package com.huanshankeji.exposedvertxsqlclient.postgresql.vertx.pgclient

import com.huanshankeji.Untested
import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient.ConnectHandlerExtra
import com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient.createGenericSqlClient
import com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient.createGenericSqlClientWithBuilder
import com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient.createGenericSqlConnection
import com.huanshankeji.vertx.pgclient.setUpConventionally
import io.vertx.core.Vertx
import io.vertx.pgclient.PgBuilder
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgConnection
import io.vertx.pgclient.impl.PgPoolOptions
import io.vertx.sqlclient.ClientBuilder
import io.vertx.sqlclient.Pool
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
    extraPgConnectOptions: PgConnectOptions.() -> Unit,
    extraPgPoolOptions: PgPoolOptions.() -> Unit,
    connectHandlerExtra: ConnectHandlerExtra
): SqlClientT =
    createGenericSqlClientWithBuilder(
        vertx,
        connectionConfig,
        clientBuilder,
        PgConnectOptions(),
        extraPgConnectOptions,
        extraPgPoolOptions,
        connectHandlerExtra,
        PgPoolOptions()
    )

fun createPgClient(
    vertx: Vertx?,
    connectionConfig: ConnectionConfig,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {},
    extraPoolOptions: PgPoolOptions.() -> Unit = {},
    connectHandlerExtra: ConnectHandlerExtra = null,
): SqlClient =
    createGenericPgClientWithBuilder(
        vertx,
        connectionConfig,
        PgBuilder.client(),
        extraPgConnectOptions,
        extraPoolOptions,
        connectHandlerExtra
    )

/**
 * [PgPoolOptions.pipelined] is enabled by default.
 * @see PgPoolOptions.setUpConventionally
 * @see createGenericSqlClient
 */
fun createPgPool(
    vertx: Vertx?,
    connectionConfig: ConnectionConfig,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {},
    extraPoolOptions: PgPoolOptions.() -> Unit = {},
    connectHandlerExtra: ConnectHandlerExtra = null,
): Pool =
    createGenericPgClientWithBuilder(
        vertx,
        connectionConfig,
        PgBuilder.pool(),
        extraPgConnectOptions,
        {
            setUpConventionally()
            extraPoolOptions()
        },
        connectHandlerExtra
    )

/**
 * @see createGenericSqlClient
 */
@Untested
suspend fun createPgConnection(
    vertx: Vertx?,
    connectionConfig: ConnectionConfig,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {},
    connectHandlerExtra: ConnectHandlerExtra = null
): PgConnection =
    createGenericSqlConnection(
        vertx,
        connectionConfig,
        PgConnection::connect,
        PgConnectOptions(),
        extraPgConnectOptions,
        connectHandlerExtra
    )
