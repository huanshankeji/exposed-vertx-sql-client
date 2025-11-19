package com.huanshankeji.exposedvertxsqlclient.db2

import com.huanshankeji.exposedvertxsqlclient.DatabaseClient
import com.huanshankeji.exposedvertxsqlclient.withTypedTransaction
import io.vertx.db2client.DB2Connection
import io.vertx.sqlclient.Pool

suspend fun <T> DatabaseClient<Pool>.withDb2Transaction(function: suspend (DatabaseClient<DB2Connection>) -> T): T =
    withTypedTransaction(function)
