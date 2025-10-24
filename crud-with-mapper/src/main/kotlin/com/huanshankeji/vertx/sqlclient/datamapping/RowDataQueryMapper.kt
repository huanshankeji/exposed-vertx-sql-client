package com.huanshankeji.vertx.sqlclient.datamapping

import io.vertx.sqlclient.Row

fun interface RowDataQueryMapper<Data : Any> {
    fun rowToData(row: Row): Data
}
