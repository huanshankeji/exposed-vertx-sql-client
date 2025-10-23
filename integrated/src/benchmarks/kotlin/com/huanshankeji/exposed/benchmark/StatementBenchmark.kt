package com.huanshankeji.exposed.benchmark

import com.huanshankeji.exposed.benchmark.table.EmptyTable
import com.huanshankeji.exposed.benchmark.table.VarcharTable
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Param
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.buildStatement
import org.jetbrains.exposed.v1.jdbc.selectAll

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
    fun createInsertStatement() = buildStatement { table.insert {} }

    @Benchmark
    fun createUpdateStatement() = buildStatement { table.update {} }

    @Benchmark
    fun deleteAllStatement() = buildStatement { table.deleteAll() }

    @Benchmark
    fun deleteWhereStatement() = buildStatement { table.deleteWhere { Op.TRUE } }
}