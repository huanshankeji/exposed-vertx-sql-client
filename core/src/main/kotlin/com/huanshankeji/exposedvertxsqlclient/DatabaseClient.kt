package com.huanshankeji.exposedvertxsqlclient

import com.huanshankeji.ExperimentalApi
import com.huanshankeji.collections.singleOrNullIfEmpty
import com.huanshankeji.kotlinx.coroutine.CoroutineAutoCloseable
import com.huanshankeji.vertx.kotlin.sqlclient.executeBatchAwaitForSqlResultSequence
import io.vertx.core.buffer.Buffer
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.core.statements.Statement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.transactions.transactionManager
import org.slf4j.LoggerFactory
import java.util.function.Function
import kotlin.Any
import kotlin.AssertionError
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Exception
import kotlin.IllegalArgumentException
import kotlin.Int
import kotlin.NotImplementedError
import kotlin.OptIn
import kotlin.Pair
import kotlin.ReplaceWith
import kotlin.String
import kotlin.also
import kotlin.collections.map
import kotlin.let
import kotlin.reflect.KClass
import kotlin.require
import kotlin.run
import kotlin.sequences.Sequence
import kotlin.sequences.map
import kotlin.to
import org.jetbrains.exposed.v1.core.Transaction as ExposedTransaction

@ExperimentalEvscApi
typealias ExposedArguments = Iterable<Pair<IColumnType<*>, Any?>>

@ExperimentalEvscApi
fun Statement<*>.singleStatementArguments() =
    arguments().singleOrNull()


@ExperimentalEvscApi
fun ExposedArguments.toVertxTuple(): Tuple =
    Tuple.wrap(map {
        val value = it.second
        when (value) {
            is EntityID<*> -> value.value
            is List<*> -> value.toTypedArray()
            else -> value
        }
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
 */
@OptIn(ExperimentalApi::class)
// TODO also consider adding `DatabaseClientConfig` as a type parameter and `PgDatabaseClientConfig` a subtype for specific dialect operations.
class DatabaseClient<out VertxSqlClientT : SqlClient>(
    val vertxSqlClient: VertxSqlClientT,
    val exposedDatabase: Database,
    val config: DatabaseClientConfig
) : CoroutineAutoCloseable {
    companion object {
        /*
        private const val FUNCTION_TRANSFORMING_ROWS_USING_PREPARED_QUERY_MAPPING_DEPRECATED_MESSAGE =
            "This function transforms rows using Vert.x's `PreparedQuery.mapping` and is deprecated. " +
                    "The Vert.x `RowSet` stores all the results in a List rather fetch as needed, so using `PreparedQuery.mapping` doesn't bring any performance benefits compared to using Kotlin's `map`. " +
                    "In addition, the Exposed `transaction` can be created on a finer-grained level only for the Kotlin `map` process from Vert.x `Row`s to Exposed `ResultRow`s."
        */
        @InternalApi
        const val SELECT_BATCH_QUERY_WITH_FIELD_SET_DEPRECATED_MESSAGE =
            "This function is buggy. The field expression set of the `fieldSet` parameter may be different from the those of the queries. Use the new overload without the `fieldSet` parameter instead."
    }

    override suspend fun close() {
        vertxSqlClient.close().coAwait()
        // How to close The Exposed `Database`?
    }

    // Alternatively, just remove the `exposedTransaction` function(s).
    /*
    @Deprecated(
        "Use `statementPreparationExposedTransaction` for preparing data for and processing the result from the Vert.x SQL Client. " +
                "Otherwise, use the `transaction` function from Exposed directly."
    )
    */
    @Deprecated(
        "Use the overload with all the transaction parameters.",
        level = DeprecationLevel.HIDDEN
    )
    fun <T> exposedTransaction(statement: ExposedTransaction.() -> T) =
        exposedTransaction(statement = statement)

    fun <T> exposedTransaction(
        // default arguments copied from `transaction`
        transactionIsolation: Int? = exposedDatabase/*?*/.transactionManager/*?*/.defaultIsolationLevel,
        readOnly: Boolean? = exposedDatabase/*?*/.transactionManager/*?*/.defaultReadOnly,
        statement: ExposedTransaction.() -> T
    ) =
        transaction(exposedDatabase, transactionIsolation, readOnly, statement)

    /**
     * @see DatabaseClientConfig.statementPreparationExposedTransactionIsolationLevel
     */
    fun <T> statementPreparationExposedTransaction(
        statement: ExposedTransaction.() -> T
    ) =
        transaction(exposedDatabase, config.statementPreparationExposedTransactionIsolationLevel, true, statement)

    @Deprecated(
        "Renamed to `statementPreparationExposedTransaction`.",
        ReplaceWith("statementPreparationExposedTransaction(statement)")
    )
    fun <T> exposedReadOnlyTransaction(
        statement: ExposedTransaction.() -> T
    ) =
        statementPreparationExposedTransaction(statement)

    private fun Statement<*>.prepareSqlAndLogIfNeeded(transaction: ExposedTransaction) =
        prepareSQL(transaction).also {
            if (config.logSql) logger.info("Prepared SQL: $it.")
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
            "org.jetbrains.exposed.v1.jdbc.SchemaUtils"
        )
    )
    suspend fun createTable(table: Table) =
        executePlainSqlUpdate(statementPreparationExposedTransaction {
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
            "org.jetbrains.exposed.v1.jdbc.SchemaUtils"
        )
    )
    suspend fun dropTable(table: Table) =
        executePlainSqlUpdate(statementPreparationExposedTransaction {
            table.dropStatement().joinSqls()
        })

    @Deprecated("Use `execute`.", ReplaceWith("execute<SqlResultT>(statement, transformQuery)"))
    suspend fun <SqlResultT : SqlResult<*>> doExecute(
        statement: Statement<*>,
        transformQuery: PreparedQuery<RowSet<Row>>.() -> PreparedQuery<SqlResultT>
    ) =
        execute(statement, transformQuery)

    /**
     * @param transformQuery transform the query by calling [PreparedQuery.mapping] and [PreparedQuery.collecting].
     */
    @ExperimentalEvscApi
    suspend /*inline*/ fun <SqlResultT : SqlResult<*>> execute(
        statement: Statement<*>,
        transformQuery: PreparedQuery<RowSet<Row>>.() -> PreparedQuery<SqlResultT>
    ): SqlResultT {
        val (sql, argTuple) = statementPreparationExposedTransaction {
            config.transformPreparedSql(statement.prepareSqlAndLogIfNeeded(this)) to
                    statement.getVertxSqlClientArgTuple()
        }
        return vertxSqlClient.preparedQuery(sql)
            .transformQuery()
            .run { if (argTuple === null) execute() else execute(argTuple) }
            .coAwait()
    }

    suspend fun executeForVertxSqlClientRowSet(statement: Statement<*>): RowSet<Row> =
        execute(statement) { this }


    /*
    @Deprecated(
        FUNCTION_TRANSFORMING_ROWS_USING_PREPARED_QUERY_MAPPING_DEPRECATED_MESSAGE,
        ReplaceWith("executeForVertxSqlClientRowSet(statement).map(rowMapper)")
    )
    */
    // no longer used by other non-experimental APIs of ours
    @ExperimentalEvscApi
    suspend fun <U> executeWithMapping(statement: Statement<*>, rowMapper: Function<Row, U>): RowSet<U> =
        execute(statement) { mapping(rowMapper) }

    // TODO temporarily kept, remove these in the future
    // If these functions are to be kept, consider renaming them to `...WithExposedTransaction` to make it clearer that an Exposed `transaction` is used.

    @Deprecated("This API is no longer used and will be removed.")
    @ExperimentalEvscApi
    fun FieldSet.getFieldExpressionSetWithTransaction() =
        statementPreparationExposedTransaction { getFieldExpressionSet() }

    @Deprecated("This function is called nowhere except `Row.toExposedResultRowWithTransaction`. Consider inlining and removing it.")
    @ExperimentalEvscApi
    fun Query.getFieldExpressionSetWithTransaction() =
        set.getFieldExpressionSetWithTransaction()

    @Deprecated(
        "This API is no longer used and will be removed. " +
                "This is also of potential poor performance if accidentally called to transform multiple rows."
    )
    @ExperimentalEvscApi
    private fun Row.toExposedResultRowWithTransaction(query: Query) =
        toExposedResultRow(query.getFieldExpressionSetWithTransaction())


    /**
     * @param withExposedTransaction whether to run the [block] within the transaction.
     */
    // old name: `runWithOptionalStatementPreparationExposedTransaction`
    @InternalApi
    inline fun <T> optionalStatementPreparationExposedTransaction(
        withExposedTransaction: Boolean, crossinline block: () -> T
    ): T =
        if (withExposedTransaction)
            statementPreparationExposedTransaction { block() }
        else
            block()

    @ExperimentalEvscApi
    fun Query.getFieldExpressionSetWithOptionalReadOnlyExposedTransaction(getFieldExpressionSetWithExposedTransaction: Boolean) =
        optionalStatementPreparationExposedTransaction(getFieldExpressionSetWithExposedTransaction) { getFieldExpressionSet() }

    /**
     * @param getFieldExpressionSetWithExposedTransaction see [DatabaseClientConfig.autoExposedTransaction]
     */
    /*
    @Deprecated(
        FUNCTION_TRANSFORMING_ROWS_USING_PREPARED_QUERY_MAPPING_DEPRECATED_MESSAGE,
        ReplaceWith("executeQuery(query).map(resultRowMapper)")
    )
    */
    suspend inline fun <Data> executeQuery(
        query: Query,
        getFieldExpressionSetWithExposedTransaction: Boolean = config.autoExposedTransaction,
        crossinline resultRowMapper: ResultRow.() -> Data
    ): RowSet<Data> =
        execute(query) {
            @OptIn(ExperimentalEvscApi::class)
            val fieldExpressionSet = query.getFieldExpressionSetWithOptionalReadOnlyExposedTransaction(
                getFieldExpressionSetWithExposedTransaction
            )
            mapping { row ->
                row.toExposedResultRow(fieldExpressionSet).resultRowMapper()
            }
        }

    /**
     * @param getFieldExpressionSetWithExposedTransaction see [DatabaseClientConfig.autoExposedTransaction]
     */
    /*
    @Deprecated(
        FUNCTION_TRANSFORMING_ROWS_USING_PREPARED_QUERY_MAPPING_DEPRECATED_MESSAGE,
        ReplaceWith("executeQuery(query, TODO())"),
        DeprecationLevel.HIDDEN
    )
    */
    suspend fun executeQuery(
        query: Query,
        getFieldExpressionSetWithExposedTransaction: Boolean = config.autoExposedTransaction
    ): RowSet<ResultRow> =
        executeQuery(query, getFieldExpressionSetWithExposedTransaction) { this }

    /**
     * An alternative API to [executeQuery] that returns a [List] instead of a [RowSet].
     * @param getFieldExpressionSetWithExposedTransaction see [DatabaseClientConfig.autoExposedTransaction]
     */
    @ExperimentalEvscApi
    suspend fun executeQueryForList(
        query: Query,
        getFieldExpressionSetWithExposedTransaction: Boolean = config.autoExposedTransaction
    ): List<ResultRow> {
        val rowSet = executeForVertxSqlClientRowSet(query)
        val fieldExpressionSet = query.getFieldExpressionSetWithOptionalReadOnlyExposedTransaction(
            getFieldExpressionSetWithExposedTransaction
        )
        return rowSet.map { row -> row.toExposedResultRow(fieldExpressionSet) }
    }

    /**
     * @return the updated row count from [RowSet.rowCount].
     */
    suspend fun executeUpdate(statement: Statement<Int>): Int =
        executeForVertxSqlClientRowSet(statement).rowCount()

    /**
     * @return whether exactly one row is updated; `false` if no row is updated; throws [SingleUpdateException] if more than one row is updated.
     */
    suspend fun executeSingleOrNoUpdate(statement: Statement<Int>): Boolean =
        executeUpdate(statement).singleOrNoUpdate()

    suspend fun executeSingleUpdate(statement: Statement<Int>) {
        val rowCount = executeUpdate(statement)
        if (rowCount != 1) throw SingleUpdateException(rowCount)
    }

    @Deprecated(
        "Use `selectExpression` in the `crud` module instead.",
        ReplaceWith(
            "selectExpression<T>(clazz, expression)", "com.huanshankeji.exposedvertxsqlclient.crud.selectExpression"
        )
    )
    suspend fun <T : Any> executeExpression(clazz: KClass<T>, expression: Expression<T?>): T? =
        throw NotImplementedError()

    @Deprecated(
        "Use `selectExpression` in the `crud` module instead.",
        ReplaceWith("selectExpression<T>(expression)", "com.huanshankeji.exposedvertxsqlclient.crud.selectExpression")
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
     * @see org.jetbrains.exposed.v1.jdbc.batchInsert
     * @see org.jetbrains.exposed.v1.jdbc.executeBatch
     * @see org.jetbrains.exposed.v1.core.statements.BatchUpdateStatement.addBatch though this function seems never used in Exposed
     * @see PreparedQuery.executeBatch
     * @see execute
     */
    @ExperimentalEvscApi
    suspend /*inline*/ fun <SqlResultT : SqlResult<*>> executeBatch(
        statements: Iterable<Statement<*>>,
        transformQuery: PreparedQuery<RowSet<Row>>.() -> PreparedQuery<SqlResultT>
    ): Sequence<SqlResultT> {
        //if (data.none()) return emptySequence() // This causes "java.lang.IllegalStateException: This sequence can be consumed only once." when `data` is a `ConstrainedOnceSequence`.

        val (sql, argTuples) = statementPreparationExposedTransaction {
            var sql: String? = null
            //var argumentTypes: List<IColumnType>? = null

            val argTuples = statements.map { statement ->
                // The `map` is currently not parallelized.

                val arguments = statement.singleStatementArguments()
                    ?: throw IllegalArgumentException("the prepared query of a batch statement should have arguments")
                if (sql === null) {
                    sql = statement.prepareSqlAndLogIfNeeded(this)
                    //argumentTypes = arguments.types()
                } else if (config.validateBatch) {
                    val currentSql = statement.prepareSQL(this)
                    require(currentSql == sql) {
                        "The statements passed should generate the same prepared SQL statement. " +
                                "However, we have got SQL statement \"$sql\" set by each previous element (at least one)" +
                                "and SQL statement \"$currentSql\" set by the current statement $statement."
                    }
                    /*
                    val currentElementArgumentTypes = arguments.types()
                    require(currentElementArgumentTypes == argumentTypes!!) {
                        "The statement after set by `setUpStatement` each time should generate the same argument types. " +
                                "However we have got argument types $argumentTypes set by each previous element (at least one)" +
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

        val vertxSqlClientSql = config.transformPreparedSql(sql)
        return vertxSqlClient.preparedQuery(vertxSqlClientSql)
            .transformQuery()
            .executeBatchAwaitForSqlResultSequence(argTuples)
    }

    @ExperimentalEvscApi
    suspend fun executeBatchForVertxSqlClientRowSetSequence(statements: Iterable<Statement<*>>): Sequence<RowSet<Row>> =
        executeBatch(statements) { this }

    @Deprecated(
        SELECT_BATCH_QUERY_WITH_FIELD_SET_DEPRECATED_MESSAGE,
        ReplaceWith("executeBatchQuery(queries, getFieldExpressionSetWithExposedTransaction, resultRowMapper)")
    )
    @ExperimentalEvscApi
    suspend inline fun <Data> executeBatchQuery(
        fieldSet: FieldSet,
        queries: Iterable<Query>,
        getFieldExpressionSetWithExposedTransaction: Boolean = config.autoExposedTransaction,
        crossinline resultRowMapper: ResultRow.() -> Data
    ): Sequence<RowSet<Data>> {
        // Bug here: the field expression set of `fieldSet` may be different from the those of the queries.
        val fieldExpressionSet =
            optionalStatementPreparationExposedTransaction(getFieldExpressionSetWithExposedTransaction) { fieldSet.getFieldExpressionSet() }
        return executeBatch(queries) {
            mapping { row -> row.toExposedResultRow(fieldExpressionSet).resultRowMapper() }
        }
    }

    /**
     * @see executeBatch
     * @param getFieldExpressionSetWithExposedTransaction also see [DatabaseClientConfig.autoExposedTransaction].
     */
    @ExperimentalEvscApi
    suspend inline fun <Data> executeBatchQuery(
        queries: Iterable<Query>,
        getFieldExpressionSetWithExposedTransaction: Boolean = config.autoExposedTransaction,
        crossinline resultRowMapper: ResultRow.() -> Data
    ): Sequence<RowSet<Data>> {
        val fieldExpressionSet =
            optionalStatementPreparationExposedTransaction(getFieldExpressionSetWithExposedTransaction) {
                queries.firstOrNull()?.getFieldExpressionSet()
            }
        return executeBatch(queries) {
            // The not-null assertion is fine here because `fieldExpressionSet` is only null when `queries` is empty, in which case the `mapper` will not be called.
            mapping { row -> row.toExposedResultRow(fieldExpressionSet!!).resultRowMapper() }
        }
    }

    @Deprecated(SELECT_BATCH_QUERY_WITH_FIELD_SET_DEPRECATED_MESSAGE, ReplaceWith("executeBatchQuery(queries)"))
    @ExperimentalEvscApi
    suspend fun executeBatchQuery(fieldSet: FieldSet, queries: Iterable<Query>): Sequence<RowSet<ResultRow>> =
        executeBatchQuery(fieldSet, queries) { this }

    /**
     * See the KDoc of the overload with `resultRowMapper` parameter.
     */
    @ExperimentalEvscApi
    suspend fun executeBatchQuery(
        queries: Iterable<Query>,
        getFieldExpressionSetWithExposedTransaction: Boolean = config.autoExposedTransaction
    ): Sequence<RowSet<ResultRow>> =
        executeBatchQuery(queries, getFieldExpressionSetWithExposedTransaction) { this }

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
    @ExperimentalEvscApi
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


/**
 * An Exposed transaction is required if the [FieldSet] contains custom functions that depend on dialects.
 */
fun FieldSet.getFieldExpressionSet() =
    /** [org.jetbrains.exposed.v1.jdbc.Query.ResultIterator.fieldIndex] */
    realFields.toSet()

/**
 * @see FieldSet.getFieldExpressionSet
 */
fun Query.getFieldExpressionSet() =
    set.getFieldExpressionSet()

/**
 * @see FieldSet.getFieldExpressionSet
 */

@Deprecated(
    "It's a rare case that only one row is transformed and this function calls `Query.getFieldExpressionSet` when transforming every row. " +
            "Call `getFieldExpressionSet` directly with or without an Exposed `transaction` yourself to have finer-grained control and slightly improve performance.",
    ReplaceWith("toExposedResultRow(query.getFieldExpressionSet())")
)
fun Row.toExposedResultRow(query: Query) =
    toExposedResultRow(query.getFieldExpressionSet())

class SingleUpdateException(rowCount: Int) : Exception("update row count: $rowCount")

fun Int.singleOrNoUpdate() =
    when (this) {
        0 -> false
        1 -> true
        else -> throw SingleUpdateException(this)
    }
