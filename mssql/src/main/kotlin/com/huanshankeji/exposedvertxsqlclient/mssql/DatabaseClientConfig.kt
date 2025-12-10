package com.huanshankeji.exposedvertxsqlclient.mssql

import com.huanshankeji.exposedvertxsqlclient.DatabaseClientConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import java.sql.Connection

/**
 * See the [DatabaseClientConfig] interface for parameter descriptions.
 */
fun MssqlDatabaseClientConfig(
    validateBatch: Boolean = true,
    logSql: Boolean = false,
    readOnlyTransactionIsolationLevel: Int? = Connection.TRANSACTION_READ_UNCOMMITTED,
    autoExposedTransaction: Boolean = false
) =
    @OptIn(ExperimentalEvscApi::class)
    DatabaseClientConfig(
        validateBatch,
        logSql,
        autoExposedTransaction,
        readOnlyTransactionIsolationLevel,
        String::transformMssqlPreparedSql
    )
