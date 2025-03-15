package com.huanshankeji.exposedvertxsqlclient.mysql

import com.huanshankeji.exposedvertxsqlclient.DatabaseClientConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi

fun MysqlDatabaseClientConfig(
    validateBatch: Boolean = true,
    logSql: Boolean = false
) =
    @OptIn(ExperimentalEvscApi::class)
    DatabaseClientConfig(validateBatch, logSql, { it })
