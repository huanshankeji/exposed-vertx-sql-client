package com.huanshankeji.exposedvertxsqlclient

import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.sqlclient.poolOptionsOf
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private inline fun createPgConnectOptions(
    mainPgConnectOptions: PgConnectOptions.() -> Unit = {},
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {},
): PgConnectOptions =
    PgConnectOptions().apply {
        cachePreparedStatements = true // This improves performance greatly so it's enabled by default.
        mainPgConnectOptions()
        extraPgConnectOptions()
    }

/*inline*/ fun <Client> createSocketPgClient(
    vertx: Vertx? = null,
    host: String, database: String, user: String, password: String,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}, poolOptions: PoolOptions = poolOptionsOf(),
    create: (Vertx?, PgConnectOptions, PoolOptions) -> Client
): Client {
    val pgConnectOptions = createPgConnectOptions({
        this.host = host
        this.database = database
        this.user = user
        this.password = password
    }, extraPgConnectOptions)

    return create(vertx, pgConnectOptions, poolOptions)
}

fun createSocketSqlClient(
    vertx: Vertx? = null,
    host: String, database: String, user: String, password: String,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}, poolOptions: PoolOptions = poolOptionsOf()
): SqlClient =
    createSocketPgClient(vertx, host, database, user, password, extraPgConnectOptions, poolOptions, PgPool::client)

fun createSocketPgPool(
    vertx: Vertx? = null,
    host: String, database: String, user: String, password: String,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}, poolOptions: PoolOptions = poolOptionsOf()
): PgPool =
    createSocketPgClient(vertx, host, database, user, password, extraPgConnectOptions, poolOptions, PgPool::pool)

/*inline*/ fun <Client> createPeerAuthenticationUnixDomainSocketPgClient(
    vertx: Vertx? = null,
    unixDomainSocketPath: String, database: String,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}, poolOptions: PoolOptions = poolOptionsOf(),
    create: (Vertx?, PgConnectOptions, PoolOptions) -> Client
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
    vertx: Vertx? = null,
    unixDomainSocketPath: String, database: String,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}, poolOptions: PoolOptions = poolOptionsOf()
): SqlClient =
    createPeerAuthenticationUnixDomainSocketPgClient(
        vertx, unixDomainSocketPath, database, extraPgConnectOptions, poolOptions, PgPool::client
    )

suspend fun createUnixDomainSocketSqlClientAndSetRole(
    vertx: Vertx? = null,
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
    vertx: Vertx? = null,
    unixDomainSocketPath: String, database: String,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}, poolOptions: PoolOptions = poolOptionsOf()
): PgPool =
    createPeerAuthenticationUnixDomainSocketPgClient(
        vertx, unixDomainSocketPath, database, extraPgConnectOptions, poolOptions, PgPool::pool
    )

fun createPeerAuthenticationUnixDomainSocketPgPoolAndSetRole(
    vertx: Vertx? = null,
    host: String, database: String, role: String,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}, poolOptions: PoolOptions = poolOptionsOf()
): PgPool =
    createPeerAuthenticationUnixDomainSocketPgPool(vertx, host, database, extraPgConnectOptions, poolOptions)
        .connectHandler {
            CoroutineScope(Dispatchers.Unconfined).launch {
                // TODO: are exceptions handled?
                it.query("SET ROLE $role").execute().await()
                /** @see Pool.connectHandler */
                it.close().await()
            }
        }
