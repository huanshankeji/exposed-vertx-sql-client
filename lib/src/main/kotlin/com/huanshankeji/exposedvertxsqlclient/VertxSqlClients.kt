package com.huanshankeji.exposedvertxsqlclient

import com.huanshankeji.Untested
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.sqlclient.poolOptionsOf
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgConnection
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.SqlConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/*private*/ inline fun createPgConnectOptions(
    mainPgConnectOptions: PgConnectOptions.() -> Unit = {},
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {},
): PgConnectOptions =
    PgConnectOptions().apply {
        cachePreparedStatements = true // This improves performance greatly so it's enabled by default.
        mainPgConnectOptions()
        extraPgConnectOptions()
    }

/*
// An extracted common `create` argument for `PgConnection`, but a suspend function has an incompatible type.
private val pgConnectionConnect: suspend (Vertx?, PgConnectOptions, Nothing?) -> PgConnection = { vertx, pgConnectOptions, _ ->
    PgConnection.connect(vertx, pgConnectOptions).await()
}
*/

suspend fun SqlConnection.executeSetRole(role: String) =
    query("SET ROLE $role").execute().await()

// TODO: use `ConnectionConfig` as the argument directly in all the following functions

inline fun <Client, PoolOptionsT : PoolOptions?> createSocketGenericPgClient(
    vertx: Vertx?,
    host: String, port: Int?, database: String, user: String, password: String,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}, poolOptions: PoolOptionsT,
    create: (Vertx?, PgConnectOptions, PoolOptionsT) -> Client
): Client {
    val pgConnectOptions = createPgConnectOptions({
        this.host = host
        port?.let { this.port = it }
        this.database = database
        this.user = user
        this.password = password
    }, extraPgConnectOptions)

    return create(vertx, pgConnectOptions, poolOptions)
}

fun createSocketPgSqlClient(
    vertx: Vertx?,
    host: String, port: Int?, database: String, user: String, password: String,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}, poolOptions: PoolOptions = poolOptionsOf()
): SqlClient =
    createSocketGenericPgClient<SqlClient, PoolOptions>(
        vertx, host, port, database, user, password, extraPgConnectOptions, poolOptions, PgPool::client
    )

fun createSocketPgPool(
    vertx: Vertx?,
    host: String, port: Int?, database: String, user: String, password: String,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}, poolOptions: PoolOptions = poolOptionsOf()
): PgPool =
    createSocketGenericPgClient<PgPool, PoolOptions>(
        vertx, host, port, database, user, password, extraPgConnectOptions, poolOptions, PgPool::pool
    )

@Untested
suspend fun createSocketPgConnection(
    vertx: Vertx?,
    host: String, port: Int?, database: String, user: String, password: String,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}
): PgConnection =
    createSocketGenericPgClient(
        vertx, host, port, database, user, password, extraPgConnectOptions, null
    ) { vertx, pgConnectOptions, _ ->
        PgConnection.connect(vertx, pgConnectOptions).await()
    }


inline fun <Client, PoolOptionsT : PoolOptions?> createPeerAuthenticationUnixDomainSocketGenericPgClient(
    vertx: Vertx?,
    unixDomainSocketPath: String, database: String,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}, poolOptions: PoolOptionsT,
    create: (Vertx?, PgConnectOptions, PoolOptionsT) -> Client
): Client {
    val pgConnectOptions = createPgConnectOptions(
        {
            host = unixDomainSocketPath
            this.database = database
            user = System.getProperty("user.name")
        }, extraPgConnectOptions
    )

    return create(vertx, pgConnectOptions, poolOptions)
}

fun createPeerAuthenticationUnixDomainSocketPgSqlClient(
    vertx: Vertx?,
    unixDomainSocketPath: String, database: String,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}, poolOptions: PoolOptions = poolOptionsOf()
): SqlClient =
    createPeerAuthenticationUnixDomainSocketGenericPgClient<SqlClient, PoolOptions>(
        vertx, unixDomainSocketPath, database, extraPgConnectOptions, poolOptions, PgPool::client
    )

suspend fun createUnixDomainSocketPgSqlClientAndSetRole(
    vertx: Vertx?,
    host: String, database: String, role: String,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}, poolOptions: PoolOptions = poolOptionsOf()
): SqlClient =
    createPeerAuthenticationUnixDomainSocketPgSqlClient(
        vertx, host, database, extraPgConnectOptions, poolOptions
    ).apply {
        // Is this done for all connections?
        query("SET ROLE $role").execute().await()
    }

fun createPeerAuthenticationUnixDomainSocketPgPool(
    vertx: Vertx?,
    unixDomainSocketPath: String, database: String,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}, poolOptions: PoolOptions = poolOptionsOf()
): PgPool =
    createPeerAuthenticationUnixDomainSocketGenericPgClient<PgPool, PoolOptions>(
        vertx, unixDomainSocketPath, database, extraPgConnectOptions, poolOptions, PgPool::pool
    )

fun createPeerAuthenticationUnixDomainSocketPgPoolAndSetRole(
    vertx: Vertx?,
    host: String, database: String, role: String,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}, poolOptions: PoolOptions = poolOptionsOf()
): PgPool =
    createPeerAuthenticationUnixDomainSocketPgPool(vertx, host, database, extraPgConnectOptions, poolOptions)
        .connectHandler {
            CoroutineScope(Dispatchers.Unconfined).launch {
                // TODO: are exceptions handled?
                it.executeSetRole(role)
                /** @see Pool.connectHandler */
                it.close().await()
            }
        }

@Untested
suspend fun createPeerAuthenticationUnixDomainSocketPgConnectionAndSetRole(
    vertx: Vertx?,
    host: String, database: String, role: String,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}
): PgConnection =
    createPeerAuthenticationUnixDomainSocketGenericPgClient(
        vertx, host, database, extraPgConnectOptions, null
    ) { vertx, pgConnectOptions, _ ->
        PgConnection.connect(vertx, pgConnectOptions).await().apply {
            executeSetRole(role)
        }
    }
