package com.huanshankeji.exposedvertxsqlclient

import io.vertx.sqlclient.SqlClient

@ExperimentalEvscApi
@EvscInternalApi
inline fun <reified T : SqlClient> DatabaseClient<*>.withVertxSqlClientCheckedCastToOrNull(): DatabaseClient<T>? =
    if (vertxSqlClient is T)
        @Suppress("UNCHECKED_CAST")
        this as DatabaseClient<T>
    else
        null

@ExperimentalEvscApi
@EvscInternalApi
inline fun <reified T : SqlClient> DatabaseClient<*>.withVertxSqlClientCheckedCastTo(): DatabaseClient<T> =
    withVertxSqlClientCheckedCastToOrNull()
        ?: throw ClassCastException("cannot cast DatabaseClient<${vertxSqlClient::class.simpleName}> to DatabaseClient<${T::class.simpleName}>")
