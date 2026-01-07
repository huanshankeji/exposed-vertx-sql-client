package com.huanshankeji.vertx.sqlclient.datamapping

import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import io.vertx.sqlclient.Row

@ExperimentalEvscApi
fun interface RowDataQueryMapper<Data : Any> {
    fun rowToData(row: Row): Data
}
