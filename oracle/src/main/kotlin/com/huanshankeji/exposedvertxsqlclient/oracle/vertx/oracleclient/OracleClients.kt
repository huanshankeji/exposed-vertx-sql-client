@file:OptIn(ExperimentalEvscApi::class)

package com.huanshankeji.exposedvertxsqlclient.oracle.vertx.oracleclient

import com.huanshankeji.Untested
import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient.CoConnectHandler
import com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient.createGenericSqlClient
import com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient.createGenericSqlClientWithBuilder
import com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient.createGenericSqlConnection
import io.vertx.core.Vertx
import io.vertx.oracleclient.OracleBuilder
import io.vertx.oracleclient.OracleConnectOptions
import io.vertx.oracleclient.OracleConnection
import io.vertx.sqlclient.ClientBuilder
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlClient

/**
 * @see createGenericSqlClient
 */
// made not inline anymore for easier debugging
@ExperimentalEvscApi
fun <SqlClientT : SqlClient, ClientBuilderT : ClientBuilder<SqlClientT>> createGenericOracleClientWithBuilder(
    vertx: Vertx?,
    connectionConfig: ConnectionConfig,
    clientBuilder: ClientBuilderT,
    extraOracleConnectOptions: OracleConnectOptions.() -> Unit,
    extraOraclePoolOptions: PoolOptions.() -> Unit,
    connectHandlerExtra: CoConnectHandler
): SqlClientT =
    createGenericSqlClientWithBuilder(
        vertx,
        connectionConfig,
        clientBuilder,
        OracleConnectOptions(),
        extraOracleConnectOptions,
        extraOraclePoolOptions,
        connectHandlerExtra,
        PoolOptions()
    )

/**
 * @see createGenericSqlClient
 */
fun createOraclePool(
    vertx: Vertx?,
    connectionConfig: ConnectionConfig,
    extraOracleConnectOptions: OracleConnectOptions.() -> Unit = {},
    extraPoolOptions: PoolOptions.() -> Unit = {},
    connectHandlerExtra: CoConnectHandler = null,
): Pool =
    createGenericOracleClientWithBuilder(
        vertx,
        connectionConfig,
        OracleBuilder.pool(),
        extraOracleConnectOptions,
        extraPoolOptions,
        connectHandlerExtra
    )

/**
 * @see createGenericSqlClient
 * @param vertx Non-null. See [OracleConnection.connect].
 */
@Untested
suspend fun createOracleConnection(
    vertx: Vertx,
    connectionConfig: ConnectionConfig,
    extraOracleConnectOptions: OracleConnectOptions.() -> Unit = {},
    connectHandlerExtra: CoConnectHandler = null
): OracleConnection =
    createGenericSqlConnection(
        vertx,
        connectionConfig,
        OracleConnection::connect,
        OracleConnectOptions(),
        extraOracleConnectOptions,
        connectHandlerExtra
    )
