package com.huanshankeji.exposedvertxsqlclient.postgresql

import com.huanshankeji.exposedvertxsqlclient.EvscInternalApi
import com.huanshankeji.exposedvertxsqlclient.transformPreparedSqlToNumbered
import org.jetbrains.exposed.v1.core.statements.Statement
import org.jetbrains.exposed.v1.core.Transaction as ExposedTransaction

/**
 * see https://vertx.io/docs/vertx-pg-client/java/#_prepared_queries
 */
@EvscInternalApi
fun String.transformPgPreparedSql(): String =
    transformPreparedSqlToNumbered { append('$') }

// TODO consider removing
// TODO context parameters
@EvscInternalApi
fun Statement<*>.getVertxPgClientPreparedSql(transaction: ExposedTransaction) =
    prepareSQL(transaction).transformPgPreparedSql()
