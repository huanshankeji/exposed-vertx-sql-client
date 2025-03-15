package com.huanshankeji.exposedvertxsqlclient.mysql

import com.huanshankeji.exposedvertxsqlclient.DatabaseClient
import com.huanshankeji.exposedvertxsqlclient.withTypedTransaction
import io.vertx.mysqlclient.MySQLConnection
import io.vertx.sqlclient.Pool

suspend fun <T> DatabaseClient<Pool>.withMySqlTransaction(function: suspend (DatabaseClient<MySQLConnection>) -> T): T =
    withTypedTransaction(function)
