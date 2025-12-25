package com.huanshankeji.exposedvertxsqlclient

import arrow.core.*
import com.huanshankeji.vertx.kotlin.coroutines.coroutineToFuture
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.SqlConnection
import kotlinx.coroutines.coroutineScope

// This file contains APIs related to transactions. Note that these are based on Vert.x SQL Client's transaction APIs and not related to Exposed's transaction APIs.


// TODO these functions related to transactions and savepoints should be moved to kotlin-common and can possibly be contributed to Vert.x

/**
 * When using this function, it's recommended to name the lambda parameter the same as the outer receiver so that the outer [DatabaseClient] is shadowed,
 * and so that you don't call the outer [DatabaseClient] without a transaction by accident.
 */
suspend fun <T> DatabaseClient<Pool>.withTransaction(function: suspend (DatabaseClient<SqlConnection>) -> T): T =
    coroutineScope {
        vertxSqlClient.withTransaction {
            coroutineToFuture { function(DatabaseClient(it, exposedDatabase, config)) }
        }.coAwait()
    }

suspend fun <SqlConnectionT : SqlConnection, T> DatabaseClient<Pool>.withTypedTransaction(function: suspend (DatabaseClient<SqlConnectionT>) -> T): T =
    withTransaction {
        @Suppress("UNCHECKED_CAST")
        function(it as DatabaseClient<SqlConnectionT>)
    }

suspend fun <SqlConnectionT : SqlConnection, T> DatabaseClient<SqlConnectionT>.withTransactionCommitOrRollback(function: suspend (DatabaseClient<SqlConnectionT>) -> Option<T>): Option<T> {
    val transaction = vertxSqlClient.begin().coAwait()
    return try {
        val result = function(this)
        when (result) {
            is Some<T> -> transaction.commit()
            is None -> transaction.rollback()
        }
        result
    } catch (e: Exception) {
        transaction.rollback()
        throw e
    }
}


val savepointNameRegex = Regex("\\w+")

private suspend fun DatabaseClient<SqlConnection>.savepoint(savepointName: String) =
    executePlainSqlUpdate("SAVEPOINT \"$savepointName\"").also { dbAssert(it == 0) }

private suspend fun DatabaseClient<SqlConnection>.rollbackToSavepoint(savepointName: String) =
    executePlainSqlUpdate("ROLLBACK TO SAVEPOINT \"$savepointName\"").also { dbAssert(it == 0) }

private suspend fun DatabaseClient<SqlConnection>.releaseSavepoint(savepointName: String) =
    executePlainSqlUpdate("RELEASE SAVEPOINT \"$savepointName\"").also { dbAssert(it == 0) }

/**
 * Not tested yet on DBs other than PostgreSQL.
 * A savepoint destroys one with the same name so be careful.
 */
@InternalApi
suspend fun <SqlConnectionT : SqlConnection, RollbackT, ReleaseT> DatabaseClient<SqlConnectionT>.withSavepointAndRollbackIfThrowsOrLeft(
    savepointName: String, function: suspend (DatabaseClient<SqlConnectionT>) -> Either<RollbackT, ReleaseT>
): Either<RollbackT, ReleaseT> {
    // Prepared query seems not to work here.

    require(savepointName.matches(savepointNameRegex))
    savepoint(savepointName)

    return try {
        val result = function(this)
        when (result) {
            is Either.Left -> rollbackToSavepoint(savepointName)
            is Either.Right -> releaseSavepoint(savepointName)
        }
        result
    } catch (e: Exception) {
        rollbackToSavepoint(savepointName)
        throw e
    }
}

@ExperimentalEvscApi
suspend fun <SqlConnectionT : SqlConnection, T> DatabaseClient<SqlConnectionT>.withSavepointAndRollbackIfThrows(
    savepointName: String, function: suspend (DatabaseClient<SqlConnectionT>) -> T
): T =
    withSavepointAndRollbackIfThrowsOrLeft(savepointName) { function(it).right() }.getOrElse { throw AssertionError() }

@ExperimentalEvscApi
suspend fun <SqlConnectionT : SqlConnection, T> DatabaseClient<SqlConnectionT>.withSavepointAndRollbackIfThrowsOrNone(
    savepointName: String, function: suspend (DatabaseClient<SqlConnectionT>) -> Option<T>
): Option<T> =
    withSavepointAndRollbackIfThrowsOrLeft(savepointName) { function(it).toEither { } }.getOrNone()

@ExperimentalEvscApi
suspend fun <SqlConnectionT : SqlConnection> DatabaseClient<SqlConnectionT>.withSavepointAndRollbackIfThrowsOrFalse(
    savepointName: String, function: suspend (DatabaseClient<SqlConnectionT>) -> Boolean
): Boolean =
    withSavepointAndRollbackIfThrowsOrLeft(savepointName) { if (function(it)) Unit.right() else Unit.left() }.isRight()
