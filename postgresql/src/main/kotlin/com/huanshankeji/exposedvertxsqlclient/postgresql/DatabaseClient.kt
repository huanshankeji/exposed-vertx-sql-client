package com.huanshankeji.exposedvertxsqlclient.postgresql

import com.huanshankeji.exposedvertxsqlclient.DatabaseClient
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.withTransaction
import io.vertx.pgclient.PgConnection
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.SqlClient
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.statements.Statement
import org.jetbrains.exposed.sql.Transaction as ExposedTransaction

class PgDatabaseClient<out VertxSqlClientT : SqlClient>(
    vertxSqlClient: VertxSqlClientT,
    exposedDatabase: Database,
    validateBatch: Boolean = true,
    logSql: Boolean = false
) : DatabaseClient<VertxSqlClientT>(vertxSqlClient, exposedDatabase, validateBatch, logSql) {
    override fun String.toVertxSqlClientPreparedSql(): String =
        @OptIn(ExperimentalEvscApi::class)
        toVertxPgClientPreparedSql()
}

@ExperimentalEvscApi
fun String.toVertxPgClientPreparedSql(): String =
    buildString(length * 2) {
        var i = 1
        for (c in this)
            if (c == '?') append('$').append(i++)
            else append(c)
    }

// TODO context parameters
@ExperimentalEvscApi
fun Statement<*>.getVertxPgClientPreparedSql(transaction: ExposedTransaction) =
    prepareSQL(transaction).toVertxPgClientPreparedSql()

suspend fun <T> PgDatabaseClient<Pool>.withPgTransaction(function: suspend (PgDatabaseClient<PgConnection>) -> T): T =
    withTransaction(::PgDatabaseClient, function)
