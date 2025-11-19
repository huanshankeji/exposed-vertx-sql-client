package com.huanshankeji.exposedvertxsqlclient.mssql

import com.huanshankeji.exposedvertxsqlclient.DatabaseClient
import com.huanshankeji.exposedvertxsqlclient.withTypedTransaction
import io.vertx.mssqlclient.MSSQLConnection
import io.vertx.sqlclient.Pool

suspend fun <T> DatabaseClient<Pool>.withMssqlTransaction(function: suspend (DatabaseClient<MSSQLConnection>) -> T): T =
    withTypedTransaction(function)
