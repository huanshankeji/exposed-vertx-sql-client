package com.huanshankeji.exposedvertxsqlclient.oracle

import com.huanshankeji.exposedvertxsqlclient.DatabaseClientConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import java.sql.Connection

/**
 * See the [DatabaseClientConfig] interface for parameter descriptions.
 *
 * Note: Oracle only supports [Connection.TRANSACTION_READ_COMMITTED] and [Connection.TRANSACTION_SERIALIZABLE]
 * isolation levels. The default is set to [Connection.TRANSACTION_READ_COMMITTED].
 */
fun OracleDatabaseClientConfig(
    validateBatch: Boolean = true,
    logSql: Boolean = false,
    readOnlyTransactionIsolationLevel: Int? = Connection.TRANSACTION_READ_COMMITTED,
    autoExposedTransaction: Boolean = false
) =
    @OptIn(ExperimentalEvscApi::class)
    DatabaseClientConfig(validateBatch, logSql, readOnlyTransactionIsolationLevel, autoExposedTransaction, { it })
