package com.huanshankeji.exposedvertxsqlclient.oracle

import com.huanshankeji.exposedvertxsqlclient.DatabaseClient
import com.huanshankeji.exposedvertxsqlclient.withTypedTransaction
import io.vertx.oracleclient.OracleConnection
import io.vertx.sqlclient.Pool

suspend fun <T> DatabaseClient<Pool>.withOracleTransaction(function: suspend (DatabaseClient<OracleConnection>) -> T): T =
    withTypedTransaction(function)
