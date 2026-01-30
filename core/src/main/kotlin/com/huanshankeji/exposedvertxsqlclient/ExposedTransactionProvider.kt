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
 * - Transaction-based: reuses a single shared transaction for better performance
 *
 * @see DatabaseExposedTransactionProvider
 * @see SharedJdbcTransactionExposedTransactionProvider
 */
@ExperimentalEvscApi
interface ExposedTransactionProvider {
    /**
     * Executes the given [statement] within an Exposed transaction context suitable for SQL statement preparation.
     *
     * The transaction is typically read-only and uses an appropriate isolation level for SQL generation.
     */
    fun <T> statementPreparationExposedTransaction(statement: ExposedTransaction.() -> T): T
}

/**
 * An [ExposedTransactionProvider] that creates a new transaction for each call using an Exposed [Database].
 *
 * This is the traditional approach where each SQL preparation call creates its own transaction.
 *
 * @param database the Exposed [Database] to use for creating transactions
 * @param isolationLevel the transaction isolation level, defaults to [Connection.TRANSACTION_READ_UNCOMMITTED]
 */
@ExperimentalEvscApi
class DatabaseExposedTransactionProvider(
    val database: Database,
    val isolationLevel: Int? = Connection.TRANSACTION_READ_UNCOMMITTED
) : ExposedTransactionProvider {
    override fun <T> statementPreparationExposedTransaction(statement: ExposedTransaction.() -> T): T =
        transaction(database, isolationLevel, true, statement)
}

/**
 * An [ExposedTransactionProvider] that reuses a single shared [JdbcTransaction] for all SQL preparation calls.
 *
 * This approach provides better performance by avoiding the overhead of creating a new transaction
 * for each SQL preparation call. The transaction is created once and reused across multiple SQL preparations.
 *
 * **Thread safety:** The shared transaction is used only for SQL statement preparation (via `prepareSQL`),
 * which is typically a read-only operation on Exposed's internal structures. However, if you plan to use
 * this provider concurrently from multiple threads, ensure that the operations performed within
 * [statementPreparationExposedTransaction] are thread-safe.
 *
 * **Note:** The transaction instance members needed for SQL preparation remain usable even after
 * the underlying connection is not actively used. The transaction is created in a read-only mode
 * suitable for SQL generation.
 *
 * @param database the Exposed [Database] to use for creating the shared transaction
 * @param isolationLevel the transaction isolation level, defaults to [Connection.TRANSACTION_READ_UNCOMMITTED]
 */
@ExperimentalEvscApi
class SharedJdbcTransactionExposedTransactionProvider(
    database: Database,
    isolationLevel: Int = Connection.TRANSACTION_READ_UNCOMMITTED
) : ExposedTransactionProvider {
    private val sharedTransaction: JdbcTransaction = 
        transaction(database, isolationLevel, true) {
            // Store reference to the current transaction for reuse
            // The transaction members needed for SQL preparation are still usable
            this
        }

    override fun <T> statementPreparationExposedTransaction(statement: ExposedTransaction.() -> T): T =
        sharedTransaction.statement()
}
