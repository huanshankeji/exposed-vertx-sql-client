package com.huanshankeji.exposed.benchmark.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object EmptyTable : Table()

object VarcharTable : LongIdTable() {
    val varcharColumn = varchar("varchar_column", 1 shl 16)
}
