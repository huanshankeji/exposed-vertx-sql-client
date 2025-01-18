package com.huanshankeji.exposedvertxsqlclient.mysql

import com.huanshankeji.exposedvertxsqlclient.DatabaseClient
import com.huanshankeji.exposedvertxsqlclient.withTypedTransaction
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.SqlConnection

suspend fun <T> DatabaseClient<Pool>.withSQLTransaction(function: suspend (DatabaseClient<SqlConnection>) -> T): T =
    withTypedTransaction(function)