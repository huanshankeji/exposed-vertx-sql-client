package com.huanshankeji.exposedvertxsqlclient

import arrow.core.*
import com.huanshankeji.vertx.kotlin.coroutines.coroutineToFuture
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.SqlConnection
import kotlinx.coroutines.coroutineScope

// This file contains APIs related to transactions. Note that these are based on Vert.x SQL Client's transaction APIs and not related to Exposed's transaction APIs.


// also consider changing `DatabaseClient<SqlConnection>` in `function` parameters to receivers
// TODO Some of these functions can be ported to kotlin-common (`kotlin-common-vertx` or a new `kotlin-common-vertx-arrow` module) and can possibly be contributed back to Vert.x


// for `DatabaseClient<SqlConnection>`

/**
 * @param function return [Either.Right] to commit the transaction, or [Either.Left] to roll back the transaction.
 */
@ExperimentalEvscApi
@JvmName("withTransactionEitherForSqlConnection")
suspend fun <SqlConnectionT : SqlConnection, RollbackResult, CommitResult> DatabaseClient<SqlConnectionT>.withTransactionEither(
    // This takes a `DatabaseClient<SqlConnectionT>` as the parameter instead of a `Transaction`, similar to the design in the Vert.x `withTransaction` API.
    function: suspend (DatabaseClient<SqlConnectionT>) -> Either<RollbackResult, CommitResult>
): Either<RollbackResult, CommitResult> {
    val transaction = vertxSqlClient.begin().coAwait()
    return try {
        val result = function(this)
        result.fold(
            { transaction.rollback().coAwait() },
            { transaction.commit().coAwait() }
        )
        result
    } catch (e: Exception) {
        try {
            transaction.rollback().coAwait()
        } catch (rollbackE: Exception) {
            e.addSuppressed(rollbackE)
        }
        throw e
    }
}

/**
 * @param function return [Some] to commit the transaction, or [None] to roll back the transaction.
 */
@Deprecated(
    "Use `withTransactionEither` instead.",
    ReplaceWith("this.withTransactionEither { function(it).toEither {} }.getOrNone()")
)
@ExperimentalEvscApi
suspend fun <SqlConnectionT : SqlConnection, T> DatabaseClient<SqlConnectionT>.withTransactionCommitOrRollback(function: suspend (DatabaseClient<SqlConnectionT>) -> Option<T>): Option<T> =
    withTransactionEither { function(it).toEither {} }.getOrNone()

@JvmName("withTransactionForSqlConnection")
suspend fun <SqlConnectionT : SqlConnection, T> DatabaseClient<SqlConnectionT>.withTransaction(function: suspend (DatabaseClient<SqlConnectionT>) -> T): T =
    @OptIn(ExperimentalEvscApi::class)
    withTransactionEither { function(it).right() }.getOrElse { throw AssertionError() }


// for `DatabaseClient<Pool>`

/**
 * When using this function, it's recommended to name the lambda parameter the same as the outer receiver so that the outer [DatabaseClient] is shadowed,
 * and so that you don't call the outer [DatabaseClient] without a transaction by accident.
 * @see Pool.withTransaction
 */
@ExperimentalEvscApi
@JvmName("withTransactionEitherForPool")
suspend fun <RollbackResult, CommitResult> DatabaseClient<Pool>.withTransactionEither(function: suspend (DatabaseClient<SqlConnection>) -> Either<RollbackResult, CommitResult>): Either<RollbackResult, CommitResult> =
    withConnectionClient { it.withTransactionEither(function) }

// consider adding `DatabaseClient<Pool>.withTypedTransactionEither`

/**
 * When using this function, it's recommended to name the lambda parameter the same as the outer receiver so that the outer [DatabaseClient] is shadowed,
 * and so that you don't call the outer [DatabaseClient] without a transaction by accident.
 * @see Pool.withTransaction
 */
@JvmName("withTransactionForPool")
suspend fun <T> DatabaseClient<Pool>.withTransaction(function: suspend (DatabaseClient<SqlConnection>) -> T): T =
    coroutineScope {
        vertxSqlClient.withTransaction {
            coroutineToFuture { function(DatabaseClient(it, config)) }
        }.coAwait()
    }

/**
 * A variant of [withTransaction] that casts the [SqlConnection] to a specific subtype.
 */
// alternative name: `withTypedConnectionTransaction`
@ExperimentalEvscApi
suspend inline fun <reified SqlConnectionT : SqlConnection, T> DatabaseClient<Pool>.withTypedTransaction(crossinline function: suspend (DatabaseClient<SqlConnectionT>) -> T): T =
    withTransaction {
        function(it.withVertxSqlClientCheckedCastTo<SqlConnectionT>())
    }


// for `DatabaseClient<*>`

/**
 * Polymorphic transaction function for `DatabaseClient<*>` with either [Pool] or [SqlConnection] as the [DatabaseClient.vertxSqlClient].
 */
@ExperimentalEvscApi
suspend fun <RollbackResult, CommitResult> DatabaseClient<*>.withTransactionEitherPolymorphic(function: suspend (DatabaseClient<SqlConnection>) -> Either<RollbackResult, CommitResult>): Either<RollbackResult, CommitResult> =
    @Suppress("UNCHECKED_CAST")
    when (vertxSqlClient) {
        is Pool -> (this as DatabaseClient<Pool>).withTransactionEither(function)
        is SqlConnection -> (this as DatabaseClient<SqlConnection>).withTransactionEither(function)
        else -> throw IllegalArgumentException(
            "Unsupported vertxSqlClient type: ${vertxSqlClient::class.qualifiedName}. " +
                    "Supported types are ${Pool::class.qualifiedName} and ${SqlConnection::class.qualifiedName}."
        )
    }

/**
 * Polymorphic transaction function for `DatabaseClient<*>` with either [Pool] or [SqlConnection] as the [DatabaseClient.vertxSqlClient].
 */
@ExperimentalEvscApi
suspend fun <T> DatabaseClient<*>.withTransactionPolymorphic(function: suspend (DatabaseClient<SqlConnection>) -> T): T =
    @Suppress("UNCHECKED_CAST")
    when (vertxSqlClient) {
        is Pool -> (this as DatabaseClient<Pool>).withTransaction(function)
        is SqlConnection -> (this as DatabaseClient<SqlConnection>).withTransaction(function)
        else -> throw IllegalArgumentException(
            "Unsupported vertxSqlClient type: ${vertxSqlClient::class.qualifiedName}. " +
                    "Supported types are ${Pool::class.qualifiedName} and ${SqlConnection::class.qualifiedName}."
        )
    }

/**
 * Polymorphic transaction function for `DatabaseClient<*>` with either [Pool] or [SqlConnection] as the [DatabaseClient.vertxSqlClient].
 */
@ExperimentalEvscApi
suspend inline fun <reified SqlConnectionT : SqlConnection, T> DatabaseClient<*>.withTypedTransactionPolymorphic(
    noinline function: suspend (DatabaseClient<SqlConnectionT>) -> T
): T =
    withTransactionPolymorphic { function(it.withVertxSqlClientCheckedCastTo()) }


private suspend fun DatabaseClient<SqlConnection>.savepoint(savepointName: String) =
    executePlainSqlUpdate("SAVEPOINT $savepointName").also { dbAssert(it == 0) }

private suspend fun DatabaseClient<SqlConnection>.rollbackToSavepoint(savepointName: String) =
    executePlainSqlUpdate("ROLLBACK TO SAVEPOINT $savepointName").also { dbAssert(it == 0) }

private suspend fun DatabaseClient<SqlConnection>.releaseSavepoint(savepointName: String) =
    executePlainSqlUpdate("RELEASE SAVEPOINT $savepointName").also { dbAssert(it == 0) }

/**
 * Currently only supported with PostgreSQL.
 * A savepoint destroys one with the same name so be careful.
 *
 * @param function return [Either.Right] to release the savepoint, or [Either.Left] to roll back to the savepoint.
 */
@ExperimentalEvscApi
suspend fun <SqlConnectionT : SqlConnection, RollbackResult, ReleaseResult> DatabaseClient<SqlConnectionT>.withSavepointEither(
    savepointName: String, function: suspend (DatabaseClient<SqlConnectionT>) -> Either<RollbackResult, ReleaseResult>
): Either<RollbackResult, ReleaseResult> {
    // Prepared query seems not to work here.

    requireSqlIdentifier(savepointName)
    savepoint(savepointName)

    return try {
        val result = function(this)
        result.fold(
            { rollbackToSavepoint(savepointName) },
            { releaseSavepoint(savepointName) }
        )
        result
    } catch (e: Exception) {
        try {
            rollbackToSavepoint(savepointName)
        } catch (rollbackE: Exception) {
            e.addSuppressed(rollbackE)
        }
        throw e
    }
}

@Deprecated("Use `withSavepointEither` instead.", ReplaceWith("this.withSavepointEither(savepointName, function)"))
@InternalApi
suspend fun <SqlConnectionT : SqlConnection, RollbackResult, ReleaseResult> DatabaseClient<SqlConnectionT>.withSavepointAndRollbackIfThrowsOrLeft(
    savepointName: String, function: suspend (DatabaseClient<SqlConnectionT>) -> Either<RollbackResult, ReleaseResult>
): Either<RollbackResult, ReleaseResult> =
    @OptIn(ExperimentalEvscApi::class)
    withSavepointEither(savepointName, function)

/**
 * @see withSavepointEither
 */
@ExperimentalEvscApi
suspend fun <SqlConnectionT : SqlConnection, T> DatabaseClient<SqlConnectionT>.withSavepoint(
    savepointName: String, function: suspend (DatabaseClient<SqlConnectionT>) -> T
): T =
    withSavepointEither(savepointName) { function(it).right() }.getOrElse { throw AssertionError() }

/**
 * An alias of [withSavepoint].
 */
@Deprecated("Use `withSavepoint` instead.", ReplaceWith("this.withSavepoint(savepointName, function)"))
@ExperimentalEvscApi
suspend fun <SqlConnectionT : SqlConnection, T> DatabaseClient<SqlConnectionT>.withSavepointAndRollbackIfThrows(
    savepointName: String, function: suspend (DatabaseClient<SqlConnectionT>) -> T
): T =
    withSavepoint(savepointName, function)

@Deprecated(
    "Use `withSavepointEither` directly instead.",
    ReplaceWith("this.withSavepointEither(savepointName) { function(it).toEither {} }.getOrNone()")
)
@ExperimentalEvscApi
suspend fun <SqlConnectionT : SqlConnection, T> DatabaseClient<SqlConnectionT>.withSavepointAndRollbackIfThrowsOrNone(
    savepointName: String, function: suspend (DatabaseClient<SqlConnectionT>) -> Option<T>
): Option<T> =
    withSavepointEither(savepointName) { function(it).toEither {} }.getOrNone()

@Deprecated(
    "Use `withSavepointEither` directly instead.",
    ReplaceWith("this.withSavepointEither(savepointName) { if (function(it)) Unit.right() else Unit.left() }.isRight()")
)
@ExperimentalEvscApi
suspend fun <SqlConnectionT : SqlConnection> DatabaseClient<SqlConnectionT>.withSavepointAndRollbackIfThrowsOrFalse(
    savepointName: String, function: suspend (DatabaseClient<SqlConnectionT>) -> Boolean
): Boolean =
    withSavepointEither(savepointName) { if (function(it)) Unit.right() else Unit.left() }.isRight()
