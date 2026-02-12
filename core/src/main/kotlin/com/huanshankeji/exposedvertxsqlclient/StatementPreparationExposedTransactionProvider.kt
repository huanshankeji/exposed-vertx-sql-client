package com.huanshankeji.exposedvertxsqlclient

import org.jetbrains.exposed.v1.core.InternalApi
import org.jetbrains.exposed.v1.core.transactions.withThreadLocalTransaction
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.Connection
import kotlin.concurrent.getOrSet
import org.jetbrains.exposed.v1.core.Transaction as ExposedTransaction

/**
 * Provider interface for creating and managing Exposed transactions used for SQL statement preparation.
 *
 * This abstraction allows for different strategies of providing transactions:
 * - Database-based: creates a new transaction for each call (traditional approach)
 * - Transaction-based: reuses a single JDBC transaction for better performance
 *
 * @see DatabaseExposedTransactionProvider
 * @see JdbcTransactionExposedTransactionProvider
 */
@ExperimentalEvscApi
interface StatementPreparationExposedTransactionProvider {
    /**
     * Executes the given [block] within an Exposed transaction context suitable for SQL statement preparation.
     *
     * The transaction is typically read-only and uses an appropriate isolation level for SQL generation.
     * The transaction is stored in ThreadLocal (for JDBC) or coroutine context (for R2DBC).
     */
    fun <T> statementPreparationExposedTransaction(block: ExposedTransaction.() -> T): T

    /**
     * Executes the given [statement] within an Exposed transaction context suitable for SQL statement preparation,
     * without storing the transaction in ThreadLocal or coroutine context.
     *
     * This variant provides explicit transaction handling and may slightly reduce some overhead.
     */
    @Deprecated("Not currently used by other declarations in this library.")
    fun <T> withExplicitOnlyStatementPreparationExposedTransaction(statement: ExposedTransaction.() -> T): T
}

/**
 * A [StatementPreparationExposedTransactionProvider] that creates a new transaction for each call using an Exposed [Database].
 *
 * This is the traditional approach where each SQL preparation call creates its own transaction.
 *
 * @param database the Exposed [Database] to use for creating transactions
 * @param transactionIsolation the transaction isolation level, defaults to [Connection.TRANSACTION_READ_UNCOMMITTED]
 */
@ExperimentalEvscApi
class DatabaseExposedTransactionProvider(
    val database: Database,
    val transactionIsolation: Int? = Connection.TRANSACTION_READ_UNCOMMITTED
) : StatementPreparationExposedTransactionProvider {
    override fun <T> statementPreparationExposedTransaction(block: ExposedTransaction.() -> T): T =
        transaction(database, transactionIsolation, true, block)

    @Deprecated("Not currently used by other declarations in this library.")
    override fun <T> withExplicitOnlyStatementPreparationExposedTransaction(block: ExposedTransaction.() -> T): T =
        statementPreparationExposedTransaction(block)
}

/**
 * TODO Update the docs here correspondingly: each thread must have a separate transaction to avoid contention/race.
 * A [StatementPreparationExposedTransactionProvider] that reuses a single [JdbcTransaction] for all SQL preparation calls.
 *
 * This approach provides better performance by avoiding the overhead of creating a new transaction
 * for each SQL preparation call. The transaction is created once and reused across multiple SQL preparations.
 *
 * **Thread safety:** The JDBC transaction is used only for SQL statement preparation (via statement building and `prepareSQL`),
 * which is typically a read-only operation on Exposed's internal structures. However, if you plan to use
 * this provider concurrently from multiple threads, ensure that the operations performed within
 * [statementPreparationExposedTransaction] are thread-safe.
 *
 * **Note:** The transaction instance members needed for SQL preparation remain usable even after
 * the underlying connection is not actively used. The transaction is created in a read-only mode
 * suitable for SQL generation.
 *
 * @param jdbcTransaction the [JdbcTransaction] to use for SQL statement preparation
 */
@ExperimentalEvscApi
class JdbcTransactionExposedTransactionProvider(
    val database: Database
) : StatementPreparationExposedTransactionProvider {
    companion object {
        @EvscInternalApi
        val evscThreadLocalTransaction = ThreadLocal<JdbcTransaction>()
    }

    /**
     * Creates a [JdbcTransaction] from a [Database].
     *
     * @param database the Exposed [Database] to use for creating the transaction
     */
    @EvscInternalApi
    fun createTransaction() =
        /*
        // alternative implementation that keeps that transaction open
        database.transactionManager.newTransaction(Connection.TRANSACTION_READ_UNCOMMITTED, true)
        */
        transaction(database, Connection.TRANSACTION_READ_UNCOMMITTED, true) {
            // Store reference to the current transaction for reuse
            // The transaction members needed for SQL preparation are still usable
            this
        }

    @EvscInternalApi
    fun getOrSetEvscThreadLocalTransaction() =
        evscThreadLocalTransaction.getOrSet { createTransaction() }

    @OptIn(InternalApi::class, EvscInternalApi::class)
    override fun <T> statementPreparationExposedTransaction(block: ExposedTransaction.() -> T): T {
        val transaction = getOrSetEvscThreadLocalTransaction()
        // Call statement directly on the transaction - it will be executed in the transaction context
        return withThreadLocalTransaction(transaction) { transaction.block() }
    }

    @Deprecated("Not currently used by other declarations in this library.")
    override fun <T> withExplicitOnlyStatementPreparationExposedTransaction(block: ExposedTransaction.() -> T): T =
        getOrSetEvscThreadLocalTransaction().block()
}
