package com.huanshankeji.exposedvertxsqlclient

import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.Connection

interface DatabaseClientConfig {
    /**
     * Whether to validate whether the batch statements have the same generated prepared SQL. It's recommended to keep this enabled for tests but disabled for production.
     */
    val validateBatch: Boolean
    val logSql: Boolean

    /**
     * Whether to always run some steps that possibly require Exposed [transaction]s in Exposed [transaction]s.
     *
     * As some Exposed APIs implicitly require a transaction and the requirements sometimes change between versions,
     * we provide this option to best suit different scenarios.
     *
     * Also see the parameters with `WithExposedTransaction` in their names, which default to this value.
     *
     * Enabling this option slightly degrades performance but reduces the likelihood of running into `java.lang.IllegalStateException: No transaction in context.`.
     */
    val autoExposedTransaction: Boolean

    val readOnlyTransactionIsolationLevel: Int?

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
    autoExposedTransaction: Boolean = false,
    readOnlyTransactionIsolationLevel: Int? = Connection.TRANSACTION_READ_UNCOMMITTED,
    crossinline exposedPreparedSqlToVertxSqlClientPreparedSql: (preparedSql: String) -> String
) =
    object : DatabaseClientConfig {
        override val validateBatch: Boolean = validateBatch
        override val logSql: Boolean = logSql
        override val autoExposedTransaction: Boolean = autoExposedTransaction
        override val readOnlyTransactionIsolationLevel: Int? = readOnlyTransactionIsolationLevel
        override fun transformPreparedSql(exposedPreparedSql: String): String =
            exposedPreparedSqlToVertxSqlClientPreparedSql(exposedPreparedSql)
    }
