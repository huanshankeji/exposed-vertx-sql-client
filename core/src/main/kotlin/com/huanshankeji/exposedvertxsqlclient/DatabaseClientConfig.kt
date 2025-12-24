package com.huanshankeji.exposedvertxsqlclient

/**
 * Backward compatibility typealias. Use [DatabaseClient.Config] instead.
 * @see DatabaseClient.Config
 */
typealias DatabaseClientConfig = DatabaseClient.Config

/**
 * Backward compatibility factory function. Use [DatabaseClient.invoke] instead.
 * See the [DatabaseClient.Config] interface for parameter descriptions.
 * @see DatabaseClient.invoke
 */
inline fun DatabaseClientConfig(
    validateBatch: Boolean = true,
    logSql: Boolean = false,
    statementPreparationExposedTransactionIsolationLevel: Int? = java.sql.Connection.TRANSACTION_READ_UNCOMMITTED,
    autoExposedTransaction: Boolean = false,
    crossinline exposedPreparedSqlToVertxSqlClientPreparedSql: (preparedSql: String) -> String
): DatabaseClient.Config =
    DatabaseClient(
        validateBatch,
        logSql,
        statementPreparationExposedTransactionIsolationLevel,
        autoExposedTransaction,
        exposedPreparedSqlToVertxSqlClientPreparedSql
    )

