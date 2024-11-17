package com.huanshankeji.exposedvertxsqlclient.postgresql

import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig.Socket
import com.huanshankeji.exposedvertxsqlclient.ConnectionType
import com.huanshankeji.exposedvertxsqlclient.DatabaseClient
import com.huanshankeji.exposedvertxsqlclient.local.LocalConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.postgresql.exposed.exposedDatabaseConnectPostgreSql
import com.huanshankeji.exposedvertxsqlclient.postgresql.vertx.pgclient.createPgPool
import com.huanshankeji.exposedvertxsqlclient.withTypedTransaction
import com.huanshankeji.os.isCurrentOsLinux
import io.vertx.core.Vertx
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgConnection
import io.vertx.pgclient.impl.PgPoolOptions
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.SqlClient
import org.jetbrains.exposed.sql.Database

// can be used for a shared Exposed `Database` among `DatabaseClient`s
fun createPgPoolDatabaseClient(
    vertx: Vertx?,
    vertxSqlClientConnectionConfig: ConnectionConfig,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}, extraPgPoolOptions: PgPoolOptions.() -> Unit = {},
    exposedDatabase: Database
): DatabaseClient<Pool> =
    DatabaseClient(
        createPgPool(vertx, vertxSqlClientConnectionConfig, extraPgConnectOptions, extraPgPoolOptions),
        exposedDatabase
    )

fun createPgPoolDatabaseClient(
    vertx: Vertx?,
    vertxSqlClientConnectionConfig: ConnectionConfig,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}, extraPgPoolOptions: PgPoolOptions.() -> Unit = {},
    exposedSocketConnectionConfig: Socket
): DatabaseClient<Pool> =
    createPgPoolDatabaseClient(
        vertx, vertxSqlClientConnectionConfig, extraPgConnectOptions, extraPgPoolOptions,
        exposedDatabaseConnectPostgreSql(exposedSocketConnectionConfig)
    )

/** It may be more efficient to use a single shared [Database] to generate SQLs for multiple [DatabaseClient]s/[SqlClient]s. */
fun createPgPoolDatabaseClient(
    vertx: Vertx?,
    vertxSqlClientConnectionType: ConnectionType, localConnectionConfig: LocalConnectionConfig,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}, extraPgPoolOptions: PgPoolOptions.() -> Unit = {},
    exposedDatabase: Database? = null
) =
    with(localConnectionConfig) {
        val connectionConfig = when (vertxSqlClientConnectionType) {
            ConnectionType.Socket -> socketConnectionConfig
            ConnectionType.UnixDomainSocketWithPeerAuthentication -> unixDomainSocketWithPeerAuthenticationConnectionConfig
        }

        if (exposedDatabase === null)
            createPgPoolDatabaseClient(
                vertx, connectionConfig, extraPgConnectOptions, extraPgPoolOptions, socketConnectionConfig
            )
        else
            createPgPoolDatabaseClient(
                vertx, connectionConfig, extraPgConnectOptions, extraPgPoolOptions, exposedDatabase
            )
    }

fun createBetterPgPoolDatabaseClient(
    vertx: Vertx?,
    localConnectionConfig: LocalConnectionConfig,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}, extraPgPoolOptions: PgPoolOptions.() -> Unit = {},
    exposedDatabase: Database? = null
) =
    createPgPoolDatabaseClient(
        vertx,
        if (isCurrentOsLinux()) ConnectionType.UnixDomainSocketWithPeerAuthentication else ConnectionType.Socket,
        localConnectionConfig,
        extraPgConnectOptions, extraPgPoolOptions,
        exposedDatabase
    )

suspend fun <T> DatabaseClient<Pool>.withPgTransaction(function: suspend (DatabaseClient<PgConnection>) -> T): T =
    withTypedTransaction(function)
