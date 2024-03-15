package com.huanshankeji.exposed.benchmark.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Table

object EmptyTable : Table()

object VarcharTable : LongIdTable() {
    val varcharColumn = varchar("varchar_column", 1 shl 16)
}
