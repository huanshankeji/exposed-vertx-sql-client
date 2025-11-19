@file:OptIn(ExperimentalEvscApi::class)

package com.huanshankeji.exposedvertxsqlclient.db2.vertx.db2client

import com.huanshankeji.Untested
import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient.CoConnectHandler
import com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient.createGenericSqlClient
import com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient.createGenericSqlClientWithBuilder
import com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient.createGenericSqlConnection
import io.vertx.core.Vertx
import io.vertx.db2client.DB2Builder
import io.vertx.db2client.DB2ConnectOptions
import io.vertx.db2client.DB2Connection
import io.vertx.sqlclient.ClientBuilder
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlClient

/**
 * @see createGenericSqlClient
 */
// made not inline anymore for easier debugging
@ExperimentalEvscApi
fun <SqlClientT : SqlClient, ClientBuilderT : ClientBuilder<SqlClientT>> createGenericDb2ClientWithBuilder(
    vertx: Vertx?,
    connectionConfig: ConnectionConfig,
    clientBuilder: ClientBuilderT,
    extraDb2ConnectOptions: DB2ConnectOptions.() -> Unit,
    extraDb2PoolOptions: PoolOptions.() -> Unit,
    connectHandlerExtra: CoConnectHandler
): SqlClientT =
    createGenericSqlClientWithBuilder(
        vertx,
        connectionConfig,
        clientBuilder,
        DB2ConnectOptions(),
        extraDb2ConnectOptions,
        extraDb2PoolOptions,
        connectHandlerExtra,
        PoolOptions()
    )

/**
 * @see createGenericSqlClient
 */
fun createDb2Pool(
    vertx: Vertx?,
    connectionConfig: ConnectionConfig,
    extraDb2ConnectOptions: DB2ConnectOptions.() -> Unit = {},
    extraPoolOptions: PoolOptions.() -> Unit = {},
    connectHandlerExtra: CoConnectHandler = null,
): Pool =
    createGenericDb2ClientWithBuilder(
        vertx,
        connectionConfig,
        DB2Builder.pool(),
        extraDb2ConnectOptions,
        extraPoolOptions,
        connectHandlerExtra
    )

/**
 * @see createGenericSqlClient
 * @param vertx Non-null. See [DB2Connection.connect].
 */
@Untested
suspend fun createDb2Connection(
    vertx: Vertx,
    connectionConfig: ConnectionConfig,
    extraDb2ConnectOptions: DB2ConnectOptions.() -> Unit = {},
    connectHandlerExtra: CoConnectHandler = null
): DB2Connection =
    createGenericSqlConnection(
        vertx,
        connectionConfig,
        DB2Connection::connect,
        DB2ConnectOptions(),
        extraDb2ConnectOptions,
        connectHandlerExtra
    )
