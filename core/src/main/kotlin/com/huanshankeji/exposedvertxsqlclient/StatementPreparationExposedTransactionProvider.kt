package com.huanshankeji.exposedvertxsqlclient

import io.vertx.core.Verticle
import org.jetbrains.exposed.v1.core.InternalApi
import org.jetbrains.exposed.v1.core.transactions.withThreadLocalTransaction
import org.jetbrains.exposed.v1.core.vendors.H2Dialect
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.inTopLevelTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.transactions.transactionManager
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
 * @see ReusingJdbcTransactionExposedTransactionProvider
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
     * Executes the given [block] within an Exposed transaction context suitable for SQL statement preparation,
     * without storing the transaction in ThreadLocal or coroutine context.
     *
     * This variant provides explicit transaction handling and may slightly reduce some overhead.
     */
    @Deprecated("Not currently used by other declarations in this library.")
    fun <T> withExplicitOnlyStatementPreparationExposedTransaction(block: ExposedTransaction.() -> T): T
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
 * A [StatementPreparationExposedTransactionProvider] that reuses/shares a single [JdbcTransaction] for SQL preparation calls.
 *
 * This approach provides better performance by avoiding the overhead of creating a new transaction
 * for each SQL preparation call. The same transaction is reused/shared across multiple SQL preparations.
 *
 * **Thread safety:** This provider holds a single [JdbcTransaction] instance with internal mutable state and is
 * not designed for concurrent use from multiple threads. Calling [statementPreparationExposedTransaction] from
 * multiple threads at the same time on the same provider instance may lead to inconsistent internal state.
 * If you need to prepare statements concurrently, create a separate
 * [ReusingJdbcTransactionExposedTransactionProvider] (and underlying [JdbcTransaction]) per thread/[Verticle].
 *
 * **Read-only behavior:** This provider is intended to be used only for SQL statement preparation (e.g., statement
 * building and `prepareSQL`) and the transaction must not be used to execute DML/DDL statements that modify the database.
 * The underlying [JdbcTransaction] may still accumulate internal state (such as caches or counters) while being
 * used for SQL generation, so it is not strictly read-only in terms of object immutability, but it should be used
 * in a way that does not perform database writes.
 *
 * Use [createWithClosedTransaction] by default when no SQL preparation failures are observed.
 * If SQL preparation requires an open transaction, try the experimental [createWithOpenTransaction].
 * Wrapping an existing [JdbcTransaction] with the primary constructor does not obtain a connection.
 * Preparation cleanup rolls back the retained transaction. The caller manages its lifetime when passing a transaction
 * to the primary constructor or using [createWithOpenTransaction].
 *
 * @param jdbcTransaction the [JdbcTransaction] to use for SQL statement preparation
 */
@ExperimentalEvscApi
class ReusingJdbcTransactionExposedTransactionProvider(
    val jdbcTransaction: JdbcTransaction
) : StatementPreparationExposedTransactionProvider {
    companion object {
        /**
         * Creates a provider by running and closing an independent read-only Exposed transaction on [database],
         * retaining its [JdbcTransaction] for SQL preparation after the transaction scope has completed.
         *
         * **Implementation note:** The transaction instance members needed for SQL preparation remain usable even after
         * the underlying connection is not actively used. This workaround depends on Exposed continuing to support
         * SQL preparation with that retained state.
         *
         * Recommended when no SQL preparation failures are observed. If SQL preparation requires an open transaction,
         * try the experimental [createWithOpenTransaction].
         *
         * Create the provider once during initialization, for example in a [Verticle]'s `start` method.
         * Initialization reads SQL preparation metadata into Exposed's [Database] cache so later preparation can reuse it.
         * This can perform blocking JDBC I/O on the first use of [database] or when Exposed applies a configured default
         * schema. Run initialization on a thread where blocking is allowed. Any acquired connection is closed before
         * this function returns.
         * The transaction is independent of any caller's active transaction, which is left untouched.
         *
         * @param database the Exposed [Database] to use for creating the transaction
         * @see createWithOpenTransaction
         */
        fun createWithClosedTransaction(database: Database): ReusingJdbcTransactionExposedTransactionProvider =
            ReusingJdbcTransactionExposedTransactionProvider(
                // Avoid reusing a caller's active transaction: preparation cleanup would roll back its writes.
                inTopLevelTransaction(database, Connection.TRANSACTION_READ_UNCOMMITTED, true) {
                    // Initialize Exposed's lazy metadata before SQL rendering reads it from the retained transaction.
                    database.identifierManager
                    database.version
                    database.fullVersion
                    database.supportsSelectForUpdate
                    if (database.dialect is H2Dialect) database.dialectMode
                    // Store reference to the current transaction for reuse
                    // The transaction members needed for SQL preparation are still usable
                    this
                }
            )

        /**
         * Creates a provider with an independent read-only [JdbcTransaction] that remains open for SQL preparation.
         *
         * Experimental alternative if [createWithClosedTransaction] fails because SQL preparation requires an open
         * transaction. It keeps the transaction available when SQL preparation needs a live connection.
         * Exposed acquires the connection lazily; SQL preparation may perform blocking JDBC I/O to read metadata.
         * Preparation cleanup also calls [JdbcTransaction.rollback], which can perform JDBC I/O on an open connection.
         * Run these operations on a thread where blocking is allowed, not on a Vert.x event-loop thread.
         *
         * The transaction is not bound to the current thread and does not reuse a caller's active transaction.
         * This factory uses the transaction manager directly; it does not run transaction-block setup such as
         * applying a configured default schema.
         *
         * The caller owns the retained transaction and must close [jdbcTransaction] after the last SQL preparation.
         *
         * @param database the Exposed [Database] to use for creating the transaction
         * @see createWithClosedTransaction
         */
        @ExperimentalEvscApi
        fun createWithOpenTransaction(database: Database): ReusingJdbcTransactionExposedTransactionProvider =
            ReusingJdbcTransactionExposedTransactionProvider(
                database.transactionManager.newTransaction(Connection.TRANSACTION_READ_UNCOMMITTED, true)
            )
    }

    @OptIn(InternalApi::class)
    override fun <T> statementPreparationExposedTransaction(block: ExposedTransaction.() -> T): T =
        try {
            // Call statement directly on the transaction - it will be executed in the transaction context
            withThreadLocalTransaction(jdbcTransaction) {
                jdbcTransaction.block()
            }
        } finally {
            // Accumulating state needs to be cleared. Otherwise, it causes progressive performance degradation for update statements as found.
            // `rollback` calls `withThreadLocalTransaction` too so it's put outside the `withThreadLocalTransaction` block above.
            /*
            `rollback` doesn't have synchronization mechanism as I checked.
            So when you share a transaction across multiple threads, they may call `rollback` and clear states concurrently.
            This hasn't caused performance or correctness issues as I have tested.
            However, to avoid potential issues, you are recommended to provide a separate `transaction` for each thread or `Verticle`.
            For library users: please also report any concurrency issues you encounter.
             */
            jdbcTransaction.rollback()
        }

    @Deprecated("Not currently used by other declarations in this library.")
    override fun <T> withExplicitOnlyStatementPreparationExposedTransaction(block: ExposedTransaction.() -> T): T =
        jdbcTransaction.block()

    /*
    // This implementation breaks Exposed's own transactions. See commit a07319e376f0a491e312136ab102a1eb28a7035c for more details.
    @ExperimentalEvscApi
    class PushAndGetPermanentThreadLocalTransaction : JdbcTransactionExposedTransactionProvider {
        constructor(jdbcTransaction: JdbcTransaction) : super(jdbcTransaction)
        constructor(database: Database) : super(database)

        @OptIn(InternalApi::class)
        override fun <T> statementPreparationExposedTransaction(block: ExposedTransaction.() -> T): T {
            val existingTransaction = ThreadLocalTransactionsStack.getTransactionOrNull() as JdbcTransaction?
            //val existingTransaction = TransactionManager.currentOrNull()
            val transaction = existingTransaction ?: run {
                ThreadLocalTransactionsStack.pushTransaction(jdbcTransaction)
                jdbcTransaction
            }
            return transaction.block()
        }
    }
    */
}

/** Compatibility wrapper for [ReusingJdbcTransactionExposedTransactionProvider]. */
@Deprecated(
    "Use ReusingJdbcTransactionExposedTransactionProvider instead.",
    ReplaceWith("ReusingJdbcTransactionExposedTransactionProvider")
)
@ExperimentalEvscApi
class JdbcTransactionExposedTransactionProvider(
    val jdbcTransaction: JdbcTransaction
) : StatementPreparationExposedTransactionProvider by ReusingJdbcTransactionExposedTransactionProvider(jdbcTransaction) {
    /**
     * Secondary constructor that creates a [JdbcTransaction] from a [Database].
     *
     * @param database the Exposed [Database] to use for creating the transaction
     */
    @Deprecated(
        "Use ReusingJdbcTransactionExposedTransactionProvider.createWithClosedTransaction(database) instead.",
        ReplaceWith("ReusingJdbcTransactionExposedTransactionProvider.createWithClosedTransaction(database)")
    )
    constructor(database: Database) : this(
        ReusingJdbcTransactionExposedTransactionProvider.createWithClosedTransaction(database).jdbcTransaction
    )

    companion object {
        @Deprecated(
            "Use ReusingJdbcTransactionExposedTransactionProvider.createWithClosedTransaction(database) instead.",
            ReplaceWith("ReusingJdbcTransactionExposedTransactionProvider.createWithClosedTransaction(database)")
        )
        fun fromDatabaseTransaction(database: Database): JdbcTransactionExposedTransactionProvider =
            JdbcTransactionExposedTransactionProvider(
                ReusingJdbcTransactionExposedTransactionProvider.createWithClosedTransaction(database).jdbcTransaction
            )
    }
}
