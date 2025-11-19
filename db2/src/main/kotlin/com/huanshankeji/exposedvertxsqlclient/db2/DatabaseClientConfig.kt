package com.huanshankeji.exposedvertxsqlclient.db2

import com.huanshankeji.exposedvertxsqlclient.DatabaseClientConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi

/**
 * See the [DatabaseClientConfig] interface for parameter descriptions.
 */
fun Db2DatabaseClientConfig(
    validateBatch: Boolean = true,
    logSql: Boolean = false,
    autoExposedTransaction: Boolean = false
) =
    @OptIn(ExperimentalEvscApi::class)
    DatabaseClientConfig(validateBatch, logSql, autoExposedTransaction, { it })
