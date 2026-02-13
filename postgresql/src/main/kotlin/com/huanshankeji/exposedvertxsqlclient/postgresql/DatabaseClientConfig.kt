package com.huanshankeji.exposedvertxsqlclient.postgresql

import com.huanshankeji.exposedvertxsqlclient.DatabaseClientConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.StatementPreparationExposedTransactionProvider

/**
 * See the [DatabaseClientConfig] interface for parameter descriptions.
 */
fun PgDatabaseClientConfig(
    @OptIn(ExperimentalEvscApi::class)
    statementPreparationExposedTransactionProvider: StatementPreparationExposedTransactionProvider,
    validateBatch: Boolean = true,
    logSql: Boolean = false,
    autoExposedTransaction: Boolean = false
) =
    @OptIn(ExperimentalEvscApi::class)
    DatabaseClientConfig(
        statementPreparationExposedTransactionProvider,
        validateBatch,
        logSql,
        autoExposedTransaction,
        String::transformPgPreparedSql
    )
