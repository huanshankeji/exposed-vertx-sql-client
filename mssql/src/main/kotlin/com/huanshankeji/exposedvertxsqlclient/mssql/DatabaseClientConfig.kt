package com.huanshankeji.exposedvertxsqlclient.mssql

import com.huanshankeji.exposedvertxsqlclient.DatabaseClientConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.StatementPreparationExposedTransactionProvider
import java.sql.Connection

/**
 * See the [DatabaseClientConfig] interface for parameter descriptions.
 */
fun MssqlDatabaseClientConfig(
    @OptIn(ExperimentalEvscApi::class)
    statementPreparationExposedTransactionProvider: StatementPreparationExposedTransactionProvider,
    validateBatch: Boolean = true,
    logSql: Boolean = false,
    statementPreparationExposedTransactionIsolationLevel: Int? = Connection.TRANSACTION_READ_UNCOMMITTED,
    autoExposedTransaction: Boolean = false
) =
    @OptIn(ExperimentalEvscApi::class)
    DatabaseClientConfig(
        statementPreparationExposedTransactionProvider,
        validateBatch,
        logSql,
        statementPreparationExposedTransactionIsolationLevel,
        autoExposedTransaction,
        String::transformMssqlPreparedSql
    )
