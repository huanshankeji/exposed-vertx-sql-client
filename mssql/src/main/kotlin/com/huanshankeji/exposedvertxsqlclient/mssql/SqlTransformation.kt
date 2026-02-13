package com.huanshankeji.exposedvertxsqlclient.mssql

import com.huanshankeji.exposedvertxsqlclient.EvscInternalApi
import com.huanshankeji.exposedvertxsqlclient.transformPreparedSqlToNumbered

/**
 * see https://vertx.io/docs/vertx-mssql-client/java/#_prepared_queries
 */
@EvscInternalApi
fun String.transformMssqlPreparedSql(): String =
    transformPreparedSqlToNumbered { append("@p") }
