package com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient

import io.vertx.sqlclient.SqlClient

// TODO remove
interface VertxSqlClientFactory<SqlClientT : SqlClient> {
    fun buildSqlClient(): SqlClientT
}