package com.huanshankeji.exposedvertxsqlclient

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.Connection
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
     * Executes the given [statement] within an Exposed transaction context suitable for SQL statement preparation.
     *
     * The transaction is typically read-only and uses an appropriate isolation level for SQL generation.
     * The transaction is stored in ThreadLocal (for JDBC) or coroutine context (for R2DBC).
     */
    fun <T> statementPreparationExposedTransaction(statement: ExposedTransaction.() -> T): T
    
    /**
     * Executes the given [statement] within an Exposed transaction context suitable for SQL statement preparation,
     * without storing the transaction in ThreadLocal or coroutine context.
     *
     * This variant provides explicit transaction handling and may be useful in scenarios where thread-local
     * storage is not desired or when more control over transaction lifecycle is needed.
     */
    fun <T> withExplicitStatementPreparationExposedTransaction(statement: ExposedTransaction.() -> T): T
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
    override fun <T> statementPreparationExposedTransaction(statement: ExposedTransaction.() -> T): T =
        transaction(database, transactionIsolation, true, statement)
    
    override fun <T> withExplicitStatementPreparationExposedTransaction(statement: ExposedTransaction.() -> T): T =
        statementPreparationExposedTransaction(statement)
}

/**
 * A [StatementPreparationExposedTransactionProvider] that reuses a single [JdbcTransaction] for all SQL preparation calls.
 *
 * This approach provides better performance by avoiding the overhead of creating a new transaction
 * for each SQL preparation call. The transaction is created once and reused across multiple SQL preparations.
 *
 * **Thread safety:** The JDBC transaction is used only for SQL statement preparation (via `prepareSQL`),
 * which is typically a read-only operation on Exposed's internal structures. However, if you plan to use
 * this provider concurrently from multiple threads, ensure that the operations performed within
 * [statementPreparationExposedTransaction] are thread-safe.
 *
 * **Note:** The transaction instance members needed for SQL preparation remain usable even after
 * the underlying connection is not actively used. The transaction is created in a read-only mode
 * suitable for SQL generation.
 *
 * You can also create instances directly using [JdbcTransactionManager.newTransaction][org.jetbrains.exposed.v1.jdbc.transactions.JdbcTransactionManager.newTransaction].
 *
 * @param jdbcTransaction the [JdbcTransaction] to use for SQL statement preparation
 */
@ExperimentalEvscApi
class JdbcTransactionExposedTransactionProvider(
    val jdbcTransaction: JdbcTransaction
) : StatementPreparationExposedTransactionProvider {
    /**
     * Secondary constructor that creates a [JdbcTransaction] from a [Database].
     *
     * @param database the Exposed [Database] to use for creating the transaction
     */
    constructor(database: Database) : this(
        transaction(database, Connection.TRANSACTION_READ_UNCOMMITTED, true) {
            // Store reference to the current transaction for reuse
            // The transaction members needed for SQL preparation are still usable
            this
        }
    )

    override fun <T> statementPreparationExposedTransaction(statement: ExposedTransaction.() -> T): T =
        // Call statement directly on the transaction - it will be executed in the transaction context
        jdbcTransaction.statement()
    
    override fun <T> withExplicitStatementPreparationExposedTransaction(statement: ExposedTransaction.() -> T): T =
        jdbcTransaction.statement()
}
