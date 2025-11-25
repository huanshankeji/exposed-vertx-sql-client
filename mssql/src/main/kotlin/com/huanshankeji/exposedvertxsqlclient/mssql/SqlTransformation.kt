package com.huanshankeji.exposedvertxsqlclient.mssql

import com.huanshankeji.exposedvertxsqlclient.InternalApi
import com.huanshankeji.exposedvertxsqlclient.transformPreparedSqlToNumbered

/**
 * see https://vertx.io/docs/vertx-mssql-client/java/#_prepared_queries
 */
@InternalApi
fun String.transformMssqlPreparedSql(): String =
    transformPreparedSqlToNumbered { append("@p") }
