package com.huanshankeji.exposed.benchmark.jdbc

import com.huanshankeji.exposed.benchmark.table.VarcharTable
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Param
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.Statement
import org.jetbrains.exposed.v1.core.statements.buildStatement
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

@State(Scope.Benchmark)
class PreparedSqlGenerationBenchmark : WithContainerizedDatabaseAndExposedDatabaseBenchmark() {
    enum class StatementEnum(val statement: Statement<*>) {
        SelectAll(VarcharTable.selectAll()),
        SelectWhere(VarcharTable.selectAll().where(VarcharTable.id eq 0L)),
        Insert(buildStatement { VarcharTable.insert { it[varcharColumn] = "string" } }),
        Update(buildStatement { VarcharTable.update({ VarcharTable.id eq 0L }) { it[varcharColumn] = "string" } }),
        DeleteAll(buildStatement { VarcharTable.deleteAll() }),
        DeleteStatement(buildStatement { VarcharTable.deleteWhere { VarcharTable.id eq 0L } })
    }

    @Param("SelectAll", "SelectWhere", "Insert", "Update", "DeleteAll", "DeleteStatement")
    lateinit var statementEnum: StatementEnum
    val statement get() = statementEnum.statement

    /*
    var transaction: Transaction? = null
    val releaseTransactionMutex = Mutex(true)

    @OptIn(DelicateCoroutinesApi::class)
    val context = newSingleThreadContext("keep transaction alive")

    override fun setUp() {
        // for debugging purposes
        fun println(x:Int) = println("break point $x")
        super.setUp()
        println(1)
        runBlocking {
            println(2)
            val setTransactionMutex = Mutex(true)
            println(3)
            CoroutineScope(context).launch {
                // doesn't work because the transaction is stored into the current thread's `ThreadLocal` for `arguments()` to use
                newSuspendedTransaction (db = database) {
                    println(4)
                    transaction = this
                    println(5)
                    setTransactionMutex.unlock()
                    println(6)
                    releaseTransactionMutex.lock()
                    println(7)
                }
            }
            println(8)
            setTransactionMutex.lock()
            println(9)
        }
        println(10)
    }


    override fun tearDown() {
        transaction = null
        releaseTransactionMutex.unlock()
        println("isActive: " + context.isActive)
        context.cancel()

        super.tearDown()
    }
    */

    companion object {
        const val `1M` = 1000_000
    }

    @Benchmark
    fun prepareSQL1M() {
        transaction(database) {
            repeat(`1M`) {
                statement.prepareSQL(this)
            }
        }
    }

    @Benchmark
    fun arguments1M() {
        transaction(database) {
            repeat(`1M`) {
                statement.arguments()
            }
        }
    }
}