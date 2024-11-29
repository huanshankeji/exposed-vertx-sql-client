package com.huanshankeji.exposed.benchmark

import com.huanshankeji.exposed.benchmark.table.EmptyTable
import com.huanshankeji.exposed.benchmark.table.VarcharTable
import com.huanshankeji.exposed.deleteAllStatement
import com.huanshankeji.exposed.deleteWhereStatement
import com.huanshankeji.exposed.insertStatement
import com.huanshankeji.exposed.updateStatement
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Param
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll

@State(Scope.Benchmark)
class StatementBenchmark : AbstractBenchmark() {
    enum class TableEnum(val table: Table) {
        EmptyTableEnum(EmptyTable), VarcharTableEnum(VarcharTable)
    }

    @Param("EmptyTableEnum", "VarcharTableEnum")
    lateinit var tableEnum: TableEnum
    val table get() = tableEnum.table

    @Benchmark
    fun createSelectStatement() = table.selectAll()

    @Benchmark
    fun createInsertStatement() = table.insertStatement {}

    @Benchmark
    fun createUpdateStatement() = table.updateStatement(null) {}

    @Benchmark
    fun deleteAllStatement() = table.deleteAllStatement()

    @Benchmark
    fun deleteWhereStatement() = table.deleteWhereStatement { Op.TRUE }
}