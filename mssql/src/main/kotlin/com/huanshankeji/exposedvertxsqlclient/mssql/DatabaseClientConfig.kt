package com.huanshankeji.exposedvertxsqlclient.mssql

import com.huanshankeji.exposedvertxsqlclient.DatabaseClient
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import java.sql.Connection

/**
 * See the [DatabaseClient.Config] interface for parameter descriptions.
 */
fun MssqlDatabaseClientConfig(
    validateBatch: Boolean = true,
    logSql: Boolean = false,
    statementPreparationExposedTransactionIsolationLevel: Int? = Connection.TRANSACTION_READ_UNCOMMITTED,
    autoExposedTransaction: Boolean = false
): DatabaseClient.Config =
    @OptIn(ExperimentalEvscApi::class)
    DatabaseClient(
        validateBatch,
        logSql,
        statementPreparationExposedTransactionIsolationLevel,
        autoExposedTransaction,
        String::transformMssqlPreparedSql
    )
