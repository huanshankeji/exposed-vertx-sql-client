package com.huanshankeji.exposedvertxsqlclient.postgresql

import com.huanshankeji.exposedvertxsqlclient.DatabaseClient
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.withTypedTransaction
import io.vertx.pgclient.PgConnection
import io.vertx.sqlclient.Pool

@ExperimentalEvscApi
suspend fun <T> DatabaseClient<Pool>.withPgTransaction(function: suspend (DatabaseClient<PgConnection>) -> T): T =
    withTypedTransaction(function)
