package com.huanshankeji.exposedvertxsqlclient

import com.huanshankeji.kotlin.use
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.SqlConnection

@ExperimentalEvscApi
suspend fun DatabaseClient<Pool>.getVertxPoolConnection(): SqlConnection =
    @Suppress("UsePropertyAccessSyntax")
    vertxSqlClient.getConnection().coAwait()

@ExperimentalEvscApi
suspend fun DatabaseClient<Pool>.getConnectionClient(): DatabaseClient<SqlConnection> =
    DatabaseClient(getVertxPoolConnection(), exposedDatabase, config)


@ExperimentalEvscApi
suspend fun <T> DatabaseClient<Pool>.withConnectionClient(
    block: suspend (DatabaseClient<SqlConnection>) -> T
): T {
    // not using `getConnectionClient().use(...)` here because `DatabaseClient.close` has the semantics of closing the Exposed `Database` though it doesn't do it now
    val connection = getVertxPoolConnection()
    return connection.use({
        block(DatabaseClient(connection, exposedDatabase, config))
    }, {
        connection.close().coAwait()
    })
}
