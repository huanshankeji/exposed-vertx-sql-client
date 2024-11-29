package com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient

import com.huanshankeji.ExperimentalApi
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.SqlConnection

// TODO consider using prepared statements
// TODO assert and return `Unit`
// TODO consider moving to "kotlin-common"
@ExperimentalApi
suspend fun SqlConnection.setRole(role: String) =
    query("SET ROLE $role").execute().coAwait()
