package com.huanshankeji.exposedvertxsqlclient

import arrow.core.*
import com.huanshankeji.ExperimentalApi
import com.huanshankeji.collections.singleOrNullIfEmpty
import com.huanshankeji.kotlinx.coroutine.CoroutineAutoCloseable
import com.huanshankeji.vertx.kotlin.coroutines.coroutineToFuture
import com.huanshankeji.vertx.kotlin.sqlclient.executeBatchAwaitForSqlResultSequence
import io.vertx.core.buffer.Buffer
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.*
import kotlinx.coroutines.coroutineScope
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.Statement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.function.Function
import kotlin.reflect.KClass
import kotlin.sequences.Sequence
import org.jetbrains.exposed.sql.Transaction as ExposedTransaction

@ExperimentalEvscApi
typealias ExposedArguments = Iterable<Pair<IColumnType<*>, Any?>>

@ExperimentalEvscApi
fun Statement<*>.singleStatementArguments() =
    arguments().singleOrNull()


@ExperimentalEvscApi
fun ExposedArguments.toVertxTuple(): Tuple =
    Tuple.wrap(map {
        val value = it.second
        if (value is EntityID<*>) value.value else value
    })

@ExperimentalEvscApi
fun ExposedArguments.types() =
    map { it.first }

/**
 * This method has to be called within an [ExposedTransaction].
 */
@ExperimentalEvscApi
fun Statement<*>.getVertxSqlClientArgTuple() =
    singleStatementArguments()?.toVertxTuple()


@InternalApi
fun dbAssert(b: Boolean) {
    if (!b)
        throw AssertionError()
}


internal val logger = LoggerFactory.getLogger(DatabaseClient::class.java)

/**
 * A wrapper client around Vert.x [SqlClient] for queries and an Exposed [Database] to generate SQLs working around the limitations of Exposed.
 *
 * @param validateBatch whether to validate whether the batch statements have the same generated prepared SQL. It's recommended to keep this enabled for tests but disabled for production.
 */
@OptIn(ExperimentalApi::class)
abstract class DatabaseClient<out VertxSqlClientT : SqlClient>(
    val vertxSqlClient: VertxSqlClientT,
    val exposedDatabase: Database,
    // TODO consider adding a `isProduction` parameter whose default depends on the runtime
    val validateBatch: Boolean = true,
    val logSql: Boolean = false
) : CoroutineAutoCloseable {
    override suspend fun close() {
        vertxSqlClient.close().coAwait()
        // How to close The Exposed `Database`?
    }

    // TODO consider splitting into 2, one with `readOnly` set to true and isolation level `NONE` / READ UNCOMMITED for SQL generation, and a normal one for Exposed execution
    // TODO also consider adding the 2 parameters `transactionIsolation` and `readOnly` with default arguments
    fun <T> exposedTransaction(statement: ExposedTransaction.() -> T) =
        transaction(exposedDatabase, statement)

    private fun Statement<*>.prepareSqlAndLogIfNeeded(transaction: ExposedTransaction) =
        prepareSQL(transaction).also {
            if (logSql) logger.info("Prepared SQL: $it.")
        }

    suspend fun executePlainSql(sql: String): RowSet<Row> =
        /** Use [SqlClient.preparedQuery] here because of [SqlConnectOptions.setCachePreparedStatements]. */
        vertxSqlClient.preparedQuery(sql).execute().coAwait()

    suspend fun executePlainSqlUpdate(sql: String): Int =
        executePlainSql(sql).rowCount()


    private fun List<String>.joinSqls(): String =
        joinToString(";\n", postfix = ";")

    /**
     * @see SchemaUtils.create
     */
    @Deprecated(
        "This function does not support analyzing dependencies among tables. Since this action is not frequently needed we can adopt the blocking approach. Use Exposed `SchemaUtils` and create multiple tables in batch instead, temporarily.",
        ReplaceWith(
            "withContext(Dispatchers.IO) { exposedTransaction { SchemaUtils.create(table) } }",
            "kotlinx.coroutines.withContext",
            "kotlinx.coroutines.Dispatchers",
            "org.jetbrains.exposed.sql.SchemaUtils"
        )
    )
    suspend fun createTable(table: Table) =
        executePlainSqlUpdate(exposedTransaction {
            //table.createStatement()
            (table.ddl + table.indices.flatMap { it.createStatement() }).joinSqls()
        })

    @Deprecated(
        "This function does not support analyzing dependencies among tables. Since this action is not frequently needed we can adopt the blocking approach. Use Exposed `SchemaUtils` and drop multiple tables in batch instead, temporarily.",
        // TODO `Dispatchers.IO`
        ReplaceWith(
            "withContext(Dispatchers.IO) { exposedTransaction { SchemaUtils.drop(table) } }",
            "kotlinx.coroutines.withContext",
            "kotlinx.coroutines.Dispatchers",
            "org.jetbrains.exposed.sql.SchemaUtils"
        )
    )
    suspend fun dropTable(table: Table) =
        executePlainSqlUpdate(exposedTransaction {
            table.dropStatement().joinSqls()
        })

    @Deprecated("Use `execute`.", ReplaceWith("execute<SqlResultT>(statement, transformQuery)"))
    suspend fun <SqlResultT : SqlResult<*>> doExecute(
        statement: Statement<*>,
        transformQuery: PreparedQuery<RowSet<Row>>.() -> PreparedQuery<SqlResultT>
    ) =
        execute(statement, transformQuery)

    abstract fun String.toVertxSqlClientPreparedSql(): String

    /**
     * @param transformQuery transform the query by calling [PreparedQuery.mapping] and [PreparedQuery.collecting].
     */
    @ExperimentalEvscApi
    suspend /*inline*/ fun <SqlResultT : SqlResult<*>> execute(
        statement: Statement<*>,
        transformQuery: PreparedQuery<RowSet<Row>>.() -> PreparedQuery<SqlResultT>
    ): SqlResultT {
        val (sql, argTuple) = exposedTransaction {
            statement.prepareSqlAndLogIfNeeded(this).toVertxSqlClientPreparedSql() to
                    statement.getVertxSqlClientArgTuple()
        }
        return vertxSqlClient.preparedQuery(sql)
            .transformQuery()
            .run { if (argTuple === null) execute() else execute(argTuple) }
            .coAwait()
    }

    suspend fun executeForVertxSqlClientRowSet(statement: Statement<*>): RowSet<Row> =
        execute(statement) { this }

    @ExperimentalEvscApi
    suspend fun <U> executeWithMapping(statement: Statement<*>, RowMapper: Function<Row, U>): RowSet<U> =
        execute(statement) { mapping(RowMapper) }

    // TODO call `getFieldExpressionSet` inside existing transactions (the ones used to prepare the query) to further optimize the performance
    @ExperimentalEvscApi
    fun FieldSet.getFieldExpressionSetWithTransaction() =
        exposedTransaction { getFieldExpressionSet() }

    @Deprecated("This function is called nowhere except `Row.toExposedResultRowWithTransaction`. Consider inlining and removing it.")
    @ExperimentalEvscApi
    fun Query.getFieldExpressionSetWithTransaction() =
        set.getFieldExpressionSetWithTransaction()

    @ExperimentalEvscApi
    fun Row.toExposedResultRowWithTransaction(query: Query) =
        toExposedResultRow(query.getFieldExpressionSetWithTransaction())

    suspend inline fun <Data> executeQuery(
        query: Query, crossinline resultRowMapper: ResultRow.() -> Data
    ): RowSet<Data> =
        executeWithMapping(query) { row -> row.toExposedResultRowWithTransaction(query).resultRowMapper() }

    suspend fun executeQuery(query: Query): RowSet<ResultRow> =
        executeQuery(query) { this }

    suspend fun executeUpdate(statement: Statement<Int>): Int =
        executeForVertxSqlClientRowSet(statement).rowCount()

    suspend fun executeSingleOrNoUpdate(statement: Statement<Int>): Boolean =
        executeUpdate(statement).singleOrNoUpdate()

    suspend fun executeSingleUpdate(statement: Statement<Int>) =
        require(executeUpdate(statement) == 1)


    @Deprecated(
        "Use `selectExpression` in the \"sql-dsl\" module instead.",
        ReplaceWith(
            "selectExpression<T>(clazz, expression)", "com.huanshankeji.exposedvertxsqlclient.sql.selectExpression"
        )
    )
    suspend fun <T : Any> executeExpression(clazz: KClass<T>, expression: Expression<T?>): T? =
        throw NotImplementedError()

    @Deprecated(
        "Use `selectExpression` in the \"sql-dsl\" module instead.",
        ReplaceWith("selectExpression<T>(expression)", "com.huanshankeji.exposedvertxsqlclient.sql.selectExpression")
    )
    suspend inline fun <reified T> executeExpression(expression: Expression<T>): T =
        throw NotImplementedError()

    suspend fun isWorking(): Boolean =
        try {
            executePlainSql("SELECT TRUE;").first().getBoolean(0)
        } catch (e: IllegalArgumentException) {
            false
        }


    /**
     * @see org.jetbrains.exposed.sql.batchInsert
     * @see org.jetbrains.exposed.sql.executeBatch
     * @see org.jetbrains.exposed.sql.statements.BatchUpdateStatement.addBatch though this function seems never used in Exposed
     * @see PreparedQuery.executeBatch
     * @see execute
     */
    @ExperimentalEvscApi
    suspend /*inline*/ fun <SqlResultT : SqlResult<*>> executeBatch(
        statements: Iterable<Statement<*>>,
        transformQuery: PreparedQuery<RowSet<Row>>.() -> PreparedQuery<SqlResultT>
    ): Sequence<SqlResultT> {
        //if (data.none()) return emptySequence() // This causes "java.lang.IllegalStateException: This sequence can be consumed only once." when `data` is a `ConstrainedOnceSequence`.

        val (sql, argTuples) = exposedTransaction {
            var sql: String? = null
            //var argumentTypes: List<IColumnType>? = null

            val argTuples = statements.map { statement ->
                // The `map` is currently not parallelized.

                val arguments = statement.singleStatementArguments()
                    ?: throw IllegalArgumentException("the prepared query of a batch statement should have arguments")
                if (sql === null) {
                    sql = statement.prepareSqlAndLogIfNeeded(this)
                    //argumentTypes = arguments.types()
                } else if (validateBatch) {
                    val currentSql = statement.prepareSQL(this)
                    require(currentSql == sql!!) {
                        "The statement after set by `setUpStatement` each time should generate the same prepared SQL statement. " +
                                "However, we have got SQL statement \"$sql\" set by each previous element" +
                                "and SQL statement \"$currentSql\" set by the current statement $statement."
                    }
                    /*
                    val currentElementArgumentTypes = arguments.types()
                    require(currentElementArgumentTypes == argumentTypes!!) {
                        "The statement after set by `setUpStatement` each time should generate the same arguments. " +
                                "However we have got argument types $argumentTypes set by each previous element" +
                                "and argument types $currentElementArgumentTypes set by the current element $element"
                    }
                    */
                }

                arguments.toVertxTuple()
            }

            sql to argTuples
        }

        if (sql === null)
            return emptySequence()

        val vertxSqlClientSql = sql.toVertxSqlClientPreparedSql()
        return vertxSqlClient.preparedQuery(vertxSqlClientSql)
            .transformQuery()
            .executeBatchAwaitForSqlResultSequence(argTuples)
    }

    @ExperimentalEvscApi
    suspend fun executeBatchForVertxSqlClientRowSetSequence(statements: Iterable<Statement<*>>): Sequence<RowSet<Row>> =
        executeBatch(statements) { this }

    /**
     * @see executeBatch
     */
    @ExperimentalEvscApi
    suspend inline fun <Data> executeBatchQuery(
        fieldSet: FieldSet, queries: Iterable<Query>, crossinline resultRowMapper: ResultRow.() -> Data
    ): Sequence<RowSet<Data>> {
        val fieldExpressionSet = fieldSet.getFieldExpressionSetWithTransaction()
        return executeBatch(queries) {
            mapping { row -> row.toExposedResultRow(fieldExpressionSet).resultRowMapper() }
        }
    }

    suspend fun executeBatchQuery(fieldSet: FieldSet, queries: Iterable<Query>): Sequence<RowSet<ResultRow>> =
        executeBatchQuery(fieldSet, queries) { this }

    /*
    TODO Consider basing it on `Sequence` instead of `Iterable` so there is less wrapping and conversion
     when mapping as sequences, such as `asSequence` and `toIterable`.
     Also consider adding both versions.
     */
    /**
     * Executes a batch of update statements, including [InsertStatement] and [UpdateStatement].
     * @see executeBatch
     * @return a sequence of the update counts of the update statements in the batch.
     */
    suspend fun executeBatchUpdate(
        statements: Iterable<Statement<Int>>,
    ): Sequence<Int> =
        executeBatch(statements) { this }.map { it.rowCount() }
}

@Deprecated("Just use `single`.", ReplaceWith("this.single()"))
fun <R> RowSet<R>.singleResult(): R =
    single()

/** "single or no" means differently here from [Iterable.singleOrNull]. */
@Deprecated(
    "Just use `singleOrNullIfEmpty` from \"kotlin-common\".",
    ReplaceWith("this.singleOrNullIfEmpty()", "com.huanshankeji.collections.singleOrNullIfEmpty")
)
fun <R> RowSet<R>.singleOrNoResult(): R? =
    singleOrNullIfEmpty()

fun Row.toExposedResultRow(fieldExpressionSet: Set<Expression<*>>) =
    ResultRow.createAndFillValues(
        fieldExpressionSet.asSequence().mapIndexed { index, expression ->
            expression to getValue(index).let {
                when (it) {
                    is Buffer -> it.bytes
                    else -> it
                }
            }
        }.toMap()
    )

private const val USE_THE_ONE_IN_DATABASE_CLIENT_BECAUSE_TRANSACTION_REQUIRED_MESSAGE =
    "Use the one in `DatabaseClient` because a transaction may be required."

/**
 * An Exposed transaction is required if the [FieldSet] contains custom functions that depend on dialects.
 */
//@Deprecated(USE_THE_ONE_IN_DATABASE_CLIENT_BECAUSE_TRANSACTION_REQUIRED_MESSAGE)
fun FieldSet.getFieldExpressionSet() =
    /** [org.jetbrains.exposed.sql.AbstractQuery.ResultIterator.fieldsIndex] */
    realFields.toSet()

/**
 * @see FieldSet.getFieldExpressionSet
 */
@Deprecated("This function is called nowhere except `Row.toExposedResultRow`. Consider inlining and removing it.")
//@Deprecated(USE_THE_ONE_IN_DATABASE_CLIENT_BECAUSE_TRANSACTION_REQUIRED_MESSAGE)
fun Query.getFieldExpressionSet() =
    set.getFieldExpressionSet()

/**
 * @see FieldSet.getFieldExpressionSet
 */
//@Deprecated(USE_THE_ONE_IN_DATABASE_CLIENT_BECAUSE_TRANSACTION_REQUIRED_MESSAGE)
fun Row.toExposedResultRow(query: Query) =
    toExposedResultRow(query.getFieldExpressionSet())

class SingleUpdateException(rowCount: Int) : Exception("update row count: $rowCount")

fun Int.singleOrNoUpdate() =
    when (this) {
        0 -> false
        1 -> true
        else -> throw SingleUpdateException(this)
    }

// TODO these functions related to transactions and savepoints should be moved to kotlin-common and can possibly be contributed to Vert.x

typealias CreateDatabaseClient<VertxSqlClientT, DatabaseClientT /*: DatabaseClient<VertxSqlClientT>*/> =
            (vertxSqlClient: VertxSqlClientT, exposedDatabase: Database, validateBatch: Boolean, logSql: Boolean) ->
        DatabaseClientT

// These functions are kind of cumbersome as HKT is not supported in Kotlin.

/**
 * When using this function, it's recommended to name the lambda parameter the same as the outer receiver so that the outer [DatabaseClient] is shadowed,
 * and so that you don't call the outer [DatabaseClient] without a transaction by accident.
 */
suspend fun <SqlConnectionT : SqlConnection, DatabaseClientT : DatabaseClient<SqlConnectionT>, T> DatabaseClient<Pool>.withTransaction(
    createDatabaseClient: CreateDatabaseClient<SqlConnectionT, DatabaseClientT>,
    function: suspend (DatabaseClientT) -> T
): T =
    coroutineScope {
        vertxSqlClient.withTransaction {
            coroutineToFuture {
                @Suppress("UNCHECKED_CAST")
                function(createDatabaseClient(it as SqlConnectionT, exposedDatabase, validateBatch, logSql))
            }
        }.coAwait()
    }

@Deprecated("Use `withTransaction` in the specific RDMBS's package instead.")
suspend fun <T> DatabaseClient<Pool>.withTransaction(function: suspend (DatabaseClient<SqlConnection>) -> T): T =
    throw AssertionError()

@Deprecated("Use `withTransaction` in the specific RDMBS's package instead.")
suspend fun <SqlConnectionT : SqlConnection, T> DatabaseClient<Pool>.withTypedTransaction(function: suspend (DatabaseClient<SqlConnectionT>) -> T): T =
    withTransaction {
        @Suppress("UNCHECKED_CAST")
        function(it as DatabaseClient<SqlConnectionT>)
    }

suspend fun <SqlConnectionT : SqlConnection, T> DatabaseClient<SqlConnectionT>.withTransactionCommitOrRollback(function: suspend (DatabaseClient<SqlConnectionT>) -> Option<T>): Option<T> {
    val transaction = vertxSqlClient.begin().coAwait()
    return try {
        val result = function(this)
        when (result) {
            is Some<T> -> transaction.commit()
            is None -> transaction.rollback()
        }
        result
    } catch (e: Exception) {
        transaction.rollback()
        throw e
    }
}

val savepointNameRegex = Regex("\\w+")

private suspend fun DatabaseClient<SqlConnection>.savepoint(savepointName: String) =
    executePlainSqlUpdate("SAVEPOINT \"$savepointName\"").also { dbAssert(it == 0) }

private suspend fun DatabaseClient<SqlConnection>.rollbackToSavepoint(savepointName: String) =
    executePlainSqlUpdate("ROLLBACK TO SAVEPOINT \"$savepointName\"").also { dbAssert(it == 0) }

private suspend fun DatabaseClient<SqlConnection>.releaseSavepoint(savepointName: String) =
    executePlainSqlUpdate("RELEASE SAVEPOINT \"$savepointName\"").also { dbAssert(it == 0) }

/**
 * Not tested yet on DBs other than PostgreSQL.
 * A savepoint destroys one with the same name so be careful.
 */
suspend fun <SqlConnectionT : SqlConnection, RollbackT, ReleaseT> DatabaseClient<SqlConnectionT>.withSavepointAndRollbackIfThrowsOrLeft(
    savepointName: String, function: suspend (DatabaseClient<SqlConnectionT>) -> Either<RollbackT, ReleaseT>
): Either<RollbackT, ReleaseT> {
    // Prepared query seems not to work here.

    require(savepointName.matches(savepointNameRegex))
    savepoint(savepointName)

    return try {
        val result = function(this)
        when (result) {
            is Either.Left -> rollbackToSavepoint(savepointName)
            is Either.Right -> releaseSavepoint(savepointName)
        }
        result
    } catch (e: Exception) {
        rollbackToSavepoint(savepointName)
        throw e
    }
}

suspend fun <SqlConnectionT : SqlConnection, T> DatabaseClient<SqlConnectionT>.withSavepointAndRollbackIfThrows(
    savepointName: String, function: suspend (DatabaseClient<SqlConnectionT>) -> T
): T =
    withSavepointAndRollbackIfThrowsOrLeft(savepointName) { function(it).right() }.getOrElse { throw AssertionError() }

suspend fun <SqlConnectionT : SqlConnection, T> DatabaseClient<SqlConnectionT>.withSavepointAndRollbackIfThrowsOrNone(
    savepointName: String, function: suspend (DatabaseClient<SqlConnectionT>) -> Option<T>
): Option<T> =
    withSavepointAndRollbackIfThrowsOrLeft(savepointName) { function(it).toEither { } }.getOrNone()

suspend fun <SqlConnectionT : SqlConnection> DatabaseClient<SqlConnectionT>.withSavepointAndRollbackIfThrowsOrFalse(
    savepointName: String, function: suspend (DatabaseClient<SqlConnectionT>) -> Boolean
): Boolean =
    withSavepointAndRollbackIfThrowsOrLeft(savepointName) { if (function(it)) Unit.right() else Unit.left() }.isRight()
