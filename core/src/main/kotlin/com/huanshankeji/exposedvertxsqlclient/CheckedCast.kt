package com.huanshankeji.exposedvertxsqlclient

import io.vertx.sqlclient.SqlClient

inline fun <reified T : SqlClient> DatabaseClient<*>.withVertxSqlClientCheckedCastTo(): DatabaseClient<T> =
    if (vertxSqlClient is T)
        @Suppress("UNCHECKED_CAST")
        this as DatabaseClient<T>
    else
        throw ClassCastException("cannot cast DatabaseClient<${vertxSqlClient::class.simpleName}> to DatabaseClient<${T::class.simpleName}>")
