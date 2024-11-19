package com.huanshankeji.exposedvertxsqlclient

import com.huanshankeji.exposedvertxsqlclient.exposed.ExposedDatabaseFactory
import com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient.VertxSqlClientFactory
import io.vertx.sqlclient.SqlClient

// TODO remove, unnecessary abstraction as a user can just call the `DatabaseClient` constructor
@ExperimentalEvscApi
interface DatabaseClientFactory<VertxSqlClientT : SqlClient> :
    VertxSqlClientFactory<VertxSqlClientT>, ExposedDatabaseFactory {
    fun createDatabaseClient(): DatabaseClient<VertxSqlClientT>
}