package com.huanshankeji.exposedvertxsqlclient

import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.Connection

/**
 * Configuration interface for [DatabaseClient] behavior.
 *
 * This interface defines settings for SQL transformation, batch validation, logging, and transaction handling.
 * Use the database-specific factory functions (e.g., `PgDatabaseClientConfig()`, `MysqlDatabaseClientConfig()`)
 * to create instances with the correct SQL transformation for each database type.
 *
 * @see DatabaseClient
 */
interface DatabaseClientConfig {
    /**
     * Whether to validate whether the batch statements have the same generated prepared SQL.
     * It's recommended to keep this enabled for tests but disabled for production.
     * Actual performance implications as tested are insignificant.
     */
    val validateBatch: Boolean

    /** Whether to log generated SQL statements. Useful for debugging. */
    val logSql: Boolean

    /**
     * The transaction isolation level used in [transaction] in [DatabaseClient.statementPreparationExposedTransaction].
     * 
     * **Note:** This is only used when using [DatabaseExposedTransactionProvider]. When using
     * [SharedJdbcTransactionExposedTransactionProvider], the isolation level is set when creating the provider.
     */
    val statementPreparationExposedTransactionIsolationLevel: Int?

    /**
     * Whether to always run some steps that possibly require Exposed [transaction]s in Exposed [transaction]s.
     *
     * As some Exposed APIs implicitly require a transaction and the requirements sometimes change between versions,
     * we provide this option to best suit different scenarios.
     *
     * Also see the parameters with `WithExposedTransaction` in their names, which default to this value.
     *
     * Enabling this option slightly degrades performance but reduces the likelihood of running into `java.lang.IllegalStateException: No transaction in context.`.
     * 
     * **Note:** When using [SharedJdbcTransactionExposedTransactionProvider], it's recommended to set this to `true`
     * since the shared transaction is always available.
     */
    val autoExposedTransaction: Boolean

    /**
     * Transform Exposed's prepared SQL to Vert.x SQL Client's prepared SQL.
     */
    fun transformPreparedSql(exposedPreparedSql: String): String
}

/**
 * See the [DatabaseClientConfig] interface for parameter descriptions.
 */
inline fun DatabaseClientConfig(
    // TODO consider adding a `isProduction` parameter whose default depends on the runtime
    validateBatch: Boolean = true,
    logSql: Boolean = false,
    statementPreparationExposedTransactionIsolationLevel: Int? = Connection.TRANSACTION_READ_UNCOMMITTED,
    autoExposedTransaction: Boolean = false,
    crossinline exposedPreparedSqlToVertxSqlClientPreparedSql: (preparedSql: String) -> String
) =
    object : DatabaseClientConfig {
        override val validateBatch: Boolean = validateBatch
        override val logSql: Boolean = logSql
        override val statementPreparationExposedTransactionIsolationLevel: Int? =
            statementPreparationExposedTransactionIsolationLevel
        override val autoExposedTransaction: Boolean = autoExposedTransaction
        override fun transformPreparedSql(exposedPreparedSql: String): String =
            exposedPreparedSqlToVertxSqlClientPreparedSql(exposedPreparedSql)
    }
