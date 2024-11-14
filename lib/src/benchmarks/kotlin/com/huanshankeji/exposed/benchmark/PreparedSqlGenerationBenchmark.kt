package com.huanshankeji.exposed.benchmark

import com.huanshankeji.exposed.benchmark.table.VarcharTable
import com.huanshankeji.exposed.deleteAllStatement
import com.huanshankeji.exposed.deleteWhereStatement
import com.huanshankeji.exposed.insertStatement
import com.huanshankeji.exposed.updateStatement
import kotlinx.benchmark.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.Statement
import org.jetbrains.exposed.sql.transactions.transaction

@State(Scope.Benchmark)
class PreparedSqlGenerationBenchmark : WithContainerizedDatabaseBenchmark() {
    enum class StatementEnum(val statement: Statement<*>) {
        SelectAll(VarcharTable.selectAll()),
        SelectWhere(VarcharTable.selectAll().where(VarcharTable.id eq 0L)),
        Insert(VarcharTable.insertStatement { it[varcharColumn] = "string" }),
        Update(VarcharTable.updateStatement({ VarcharTable.id eq 0L }) {
            it[varcharColumn] = "string"
        }),
        DeleteAll(VarcharTable.deleteAllStatement()),
        DeleteStatement(VarcharTable.deleteWhereStatement { VarcharTable.id eq 0L })
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