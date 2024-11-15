package com.huanshankeji.exposedvertxsqlclient

import com.huanshankeji.Untested
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.coAwait
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

@PublishedApi
internal inline fun createPgConnectOptions(
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
    query("SET ROLE $role").execute().coAwait()

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

private const val PG_CLIENT_DEPRECATED_MESSAGE =
    "The dependent `PgPool` is deprecated. Just use `Pool` with `pipelined` enabled, which is the difference between `PgPool.client` and `PgPool.pool` as I have found out."

@Deprecated(PG_CLIENT_DEPRECATED_MESSAGE, ReplaceWith("createSocketPgPool"))
fun createSocketPgSqlClient(
    vertx: Vertx?,
    host: String, port: Int?, database: String, user: String, password: String,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}, poolOptions: PoolOptions = poolOptionsOf()
): SqlClient =
    @Suppress("DEPRECATION")
    createSocketGenericPgClient<SqlClient, PoolOptions>(
        vertx, host, port, database, user, password, extraPgConnectOptions, poolOptions, PgPool::client
    )

fun createSocketPgPool(
    vertx: Vertx?,
    host: String, port: Int?, database: String, user: String, password: String,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}, poolOptions: PoolOptions = poolOptionsOf()
): Pool =
    createSocketGenericPgClient<Pool, PoolOptions>(
        vertx, host, port, database, user, password, extraPgConnectOptions, poolOptions, Pool::pool
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
        PgConnection.connect(vertx, pgConnectOptions).coAwait()
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

@Deprecated(PG_CLIENT_DEPRECATED_MESSAGE, ReplaceWith("createPeerAuthenticationUnixDomainSocketPgPool"))
fun createPeerAuthenticationUnixDomainSocketPgSqlClient(
    vertx: Vertx?,
    unixDomainSocketPath: String, database: String,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}, poolOptions: PoolOptions = poolOptionsOf()
): SqlClient =
    @Suppress("DEPRECATION")
    createPeerAuthenticationUnixDomainSocketGenericPgClient<SqlClient, PoolOptions>(
        vertx, unixDomainSocketPath, database, extraPgConnectOptions, poolOptions, PgPool::client
    )

@Deprecated(PG_CLIENT_DEPRECATED_MESSAGE, ReplaceWith("createPeerAuthenticationUnixDomainSocketPgPoolAndSetRole"))
suspend fun createUnixDomainSocketPgSqlClientAndSetRole(
    vertx: Vertx?,
    host: String, database: String, role: String,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}, poolOptions: PoolOptions = poolOptionsOf()
): SqlClient =
    createPeerAuthenticationUnixDomainSocketPgSqlClient(
        vertx, host, database, extraPgConnectOptions, poolOptions
    ).apply {
        // Is this done for all connections?
        query("SET ROLE $role").execute().coAwait()
    }

fun createPeerAuthenticationUnixDomainSocketPgPool(
    vertx: Vertx?,
    unixDomainSocketPath: String, database: String,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}, poolOptions: PoolOptions = poolOptionsOf()
): Pool =
    createPeerAuthenticationUnixDomainSocketGenericPgClient<Pool, PoolOptions>(
        vertx, unixDomainSocketPath, database, extraPgConnectOptions, poolOptions, Pool::pool
    )

fun createPeerAuthenticationUnixDomainSocketPgPoolAndSetRole(
    vertx: Vertx?,
    host: String, database: String, role: String,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}, poolOptions: PoolOptions = poolOptionsOf()
): Pool =
    createPeerAuthenticationUnixDomainSocketPgPool(vertx, host, database, extraPgConnectOptions, poolOptions)
        .connectHandler {
            CoroutineScope(Dispatchers.Unconfined).launch {
                // TODO: are exceptions handled?
                it.executeSetRole(role)
                /** @see Pool.connectHandler */
                it.close().coAwait()
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
        PgConnection.connect(vertx, pgConnectOptions).coAwait().apply {
            executeSetRole(role)
        }
    }
