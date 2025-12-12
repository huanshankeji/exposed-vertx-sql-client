package com.huanshankeji.exposedvertxsqlclient.mysql

import com.huanshankeji.exposedvertxsqlclient.DatabaseClientConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import java.sql.Connection

/**
 * See the [DatabaseClientConfig] interface for parameter descriptions.
 */
fun MysqlDatabaseClientConfig(
    validateBatch: Boolean = true,
    logSql: Boolean = false,
    statementPreparationTransactionIsolationLevel: Int? = Connection.TRANSACTION_READ_UNCOMMITTED,
    autoExposedTransaction: Boolean = false
) =
    @OptIn(ExperimentalEvscApi::class)
    DatabaseClientConfig(
        validateBatch,
        logSql,
        statementPreparationTransactionIsolationLevel,
        autoExposedTransaction,
        { it }
    )
