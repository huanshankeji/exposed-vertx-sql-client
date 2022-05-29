package com.huanshankeji.exposedvertxsqlclient

import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.pgclient.pgConnectOptionsOf
import io.vertx.kotlin.sqlclient.poolOptionsOf
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

inline fun <Client> createSocketPgClient(
    vertx: Vertx? = null,
    host: String, database: String, user: String, password: String,
    create: (Vertx?, PgConnectOptions, PoolOptions) -> Client
): Client {
    val pgConnectOptions = pgConnectOptionsOf(
        host = host,
        database = database,
        user = user,
        password = password
    )
    val poolOptions = poolOptionsOf()

    return create(vertx, pgConnectOptions, poolOptions)
}

fun createSocketSqlClient(
    vertx: Vertx? = null,
    host: String, database: String, user: String, password: String
): SqlClient =
    createSocketPgClient(vertx, host, database, user, password, PgPool::client)

fun createSocketPgPool(
    vertx: Vertx? = null,
    host: String, database: String, user: String, password: String
): PgPool =
    createSocketPgClient(vertx, host, database, user, password, PgPool::pool)

inline fun <Client> createPeerAuthenticationUnixDomainSocketPgClient(
    vertx: Vertx? = null,
    unixDomainSocketPath: String, database: String,
    create: (Vertx?, PgConnectOptions, PoolOptions) -> Client
): Client {
    val pgConnectOptions = pgConnectOptionsOf(
        host = unixDomainSocketPath,
        database = database,
        user = System.getProperty("user.name")
    )
    val poolOptions = poolOptionsOf()

    return create(vertx, pgConnectOptions, poolOptions)
}

fun createPeerAuthenticationUnixDomainSocketSqlClient(
    vertx: Vertx? = null,
    unixDomainSocketPath: String, database: String
): SqlClient =
    createPeerAuthenticationUnixDomainSocketPgClient(vertx, unixDomainSocketPath, database, PgPool::client)

suspend fun createUnixDomainSocketSqlClientAndSetRole(
    vertx: Vertx? = null,
    host: String, database: String, role: String
): SqlClient =
    createPeerAuthenticationUnixDomainSocketSqlClient(vertx, host, database).apply {
        // Is this done for all connections?
        query("SET ROLE $role").execute().await()
    }


fun createPeerAuthenticationUnixDomainSocketPgPool(
    vertx: Vertx? = null,
    unixDomainSocketPath: String, database: String
): PgPool =
    createPeerAuthenticationUnixDomainSocketPgClient(vertx, unixDomainSocketPath, database, PgPool::pool)

fun createPeerAuthenticationUnixDomainSocketPgPoolAndSetRole(
    vertx: Vertx? = null,
    host: String, database: String, role: String
): PgPool =
    createPeerAuthenticationUnixDomainSocketPgPool(vertx, host, database)
        .connectHandler {
            //CoroutineScope(vertx?.dispatcher() ?: Dispatchers.Unconfined).launch {
            CoroutineScope(Dispatchers.Unconfined).launch {
                it.query("SET ROLE $role").execute().await()
                /** @see Pool.connectHandler */
                it.close().await()
            }
        }
