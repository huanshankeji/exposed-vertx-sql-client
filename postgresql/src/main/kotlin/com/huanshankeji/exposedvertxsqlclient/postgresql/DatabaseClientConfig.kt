package com.huanshankeji.exposedvertxsqlclient.postgresql

import com.huanshankeji.exposedvertxsqlclient.DatabaseClientConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi

/**
 * See the [DatabaseClientConfig] interface for parameter descriptions.
 */
fun PgDatabaseClientConfig(
    validateBatch: Boolean = true,
    logSql: Boolean = false,
    autoExposedTransaction: Boolean = false
) =
    @OptIn(ExperimentalEvscApi::class)
    DatabaseClientConfig(validateBatch, logSql, autoExposedTransaction, String::transformPgPreparedSql)
