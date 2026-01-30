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
 * **Note:** The transaction's connection should be disconnected after creation since it's only
 * used for SQL generation, not for actual database operations. The transaction instance members
 * needed for SQL preparation remain usable even after the connection is disconnected.
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
            // Get the current transaction and store it
            // Close the connection since we only need the transaction for SQL preparation
            connection.close()
            this
        }

    override fun <T> statementPreparationExposedTransaction(statement: ExposedTransaction.() -> T): T =
        sharedTransaction.statement()
}
