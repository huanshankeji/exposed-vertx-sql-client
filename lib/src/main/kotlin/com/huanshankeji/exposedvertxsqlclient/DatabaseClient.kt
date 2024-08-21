package com.huanshankeji.exposedvertxsqlclient

import arrow.core.*
import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig.Socket
import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig.UnixDomainSocketWithPeerAuthentication
import com.huanshankeji.exposedvertxsqlclient.sql.selectExpression
import com.huanshankeji.os.isOSLinux
import com.huanshankeji.vertx.kotlin.coroutines.coroutineToFuture
import com.huanshankeji.vertx.kotlin.sqlclient.executeBatchAwaitForSqlResultSequence
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.sqlclient.poolOptionsOf
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgConnection
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.*
import kotlinx.coroutines.coroutineScope
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.Transaction
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


@ExperimentalEvscApi
fun String.toVertxPgClientPreparedSql(): String {
    val stringBuilder = StringBuilder(length * 2)
    var i = 1
    for (c in this)
        if (c == '?') stringBuilder.append('$').append(i++)
        else stringBuilder.append(c)
    return stringBuilder.toString()
}

// TODO: context receivers
@ExperimentalEvscApi
fun Statement<*>.getVertxPgClientPreparedSql(transaction: ExposedTransaction) =
    prepareSQL(transaction).toVertxPgClientPreparedSql()


internal fun dbAssert(b: Boolean) {
    if (!b)
        throw AssertionError()
}


internal val logger = LoggerFactory.getLogger(DatabaseClient::class.java)

/**
 * A wrapper client around Vert.x [SqlClient] for queries and an Exposed [Database] to generate SQLs working around the limitations of Exposed.
 *
 * @param validateBatch whether to validate whether the batch statements have the same generated prepared SQL.
 */
@OptIn(ExperimentalEvscApi::class)
class DatabaseClient<out VertxSqlClient : SqlClient>(
    val vertxSqlClient: VertxSqlClient,
    val exposedDatabase: Database,
    val validateBatch: Boolean = true,
    val logSql: Boolean = false
) {
    suspend fun close() {
        vertxSqlClient.close().coAwait()
        // How to close The Exposed `Database`?
    }

    fun <T> exposedTransaction(statement: ExposedTransaction.() -> T) =
        transaction(exposedDatabase, statement)

    private fun Statement<*>.prepareSqlAndLogIfNeeded(transaction: Transaction) =
        prepareSQL(transaction).also {
            if (logSql) logger.info("Prepared SQL: $it.")
        }

    suspend fun executePlainSql(sql: String): RowSet<Row> =
        /** Use [SqlClient.preparedQuery] here because of [PgConnectOptions.setCachePreparedStatements]. */
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
        ReplaceWith("exposedTransaction { SchemaUtils.create(table) }", "org.jetbrains.exposed.sql.SchemaUtils")
    )
    suspend fun createTable(table: Table) =
        executePlainSqlUpdate(exposedTransaction {
            //table.createStatement()
            (table.ddl + table.indices.flatMap { it.createStatement() }).joinSqls()
        })

    @Deprecated(
        "This function does not support analyzing dependencies among tables. Since this action is not frequently needed we can adopt the blocking approach. Use Exposed `SchemaUtils` and drop multiple tables in batch instead, temporarily.",
        ReplaceWith("exposedTransaction { SchemaUtils.drop(table) }", "org.jetbrains.exposed.sql.SchemaUtils")
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

    /**
     * @param transformQuery transform the query by calling [PreparedQuery.mapping] and [PreparedQuery.collecting].
     */
    @ExperimentalEvscApi
    suspend /*inline*/ fun <SqlResultT : SqlResult<*>> execute(
        statement: Statement<*>,
        transformQuery: PreparedQuery<RowSet<Row>>.() -> PreparedQuery<SqlResultT>
    ): SqlResultT {
        val (sql, argTuple) = exposedTransaction {
            statement.prepareSqlAndLogIfNeeded(this).toVertxPgClientPreparedSql() to
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

    suspend inline fun <Data> executeQuery(
        query: Query, crossinline resultRowMapper: ResultRow.() -> Data
    ): RowSet<Data> =
        executeWithMapping(query) { row -> row.toExposedResultRow(query).resultRowMapper() }

    suspend fun executeQuery(query: Query): RowSet<ResultRow> =
        executeQuery(query) { this }

    suspend fun executeUpdate(statement: Statement<Int>): Int =
        executeForVertxSqlClientRowSet(statement).rowCount()

    suspend fun executeSingleOrNoUpdate(statement: Statement<Int>): Boolean =
        executeUpdate(statement).singleOrNoUpdate()

    suspend fun executeSingleUpdate(statement: Statement<Int>) =
        require(executeUpdate(statement) == 1)


    @Deprecated(
        "Use `selectExpression` instead`",
        ReplaceWith(
            "selectExpression<T>(clazz, expression)", "com.huanshankeji.exposedvertxsqlclient.sql.selectExpression"
        )
    )
    suspend fun <T : Any> executeExpression(clazz: KClass<T>, expression: Expression<T?>): T? =
        selectExpression(clazz, expression)

    @Deprecated(
        "Use `selectExpression` instead`",
        ReplaceWith("selectExpression<T>(expression)", "com.huanshankeji.exposedvertxsqlclient.sql.selectExpression")
    )
    suspend inline fun <reified T> executeExpression(expression: Expression<T>): T =
        selectExpression(expression)

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

        val pgSql = sql.toVertxPgClientPreparedSql()
        return vertxSqlClient.preparedQuery(pgSql)
            .transformQuery()
            .executeBatchAwaitForSqlResultSequence(argTuples)
    }

    @ExperimentalEvscApi
    suspend fun executeBatchForVertxSqlClientRowSetSequence(statements: Iterable<Statement<*>>): Sequence<RowSet<Row>> =
        executeBatch(statements) { this }

    @ExperimentalEvscApi
    suspend inline fun <Data> executeBatchQuery(
        fieldSet: FieldSet, queries: Iterable<Query>, crossinline resultRowMapper: ResultRow.() -> Data
    ): Sequence<RowSet<Data>> {
        val fieldExpressionSet = fieldSet.getFieldExpressionSet()
        return executeBatch(queries) {
            mapping { row -> row.toExposedResultRow(fieldExpressionSet).resultRowMapper() }
        }
    }

    suspend fun executeBatchQuery(fieldSet: FieldSet, queries: Iterable<Query>): Sequence<RowSet<ResultRow>> =
        executeBatchQuery(fieldSet, queries) { this }

    /**
     * Executes a batch of update statements, including [InsertStatement] and [UpdateStatement].
     * @see org.jetbrains.exposed.sql.batchInsert
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

// TODO consider moving into "kotlin-common" and renaming to "singleOrZero"
/** "single or no" means differently here from [Iterable.singleOrNull]. */
fun <R> RowSet<R>.singleOrNoResult(): R? =
    if (none()) null else single()

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

fun FieldSet.getFieldExpressionSet() =
    /** [org.jetbrains.exposed.sql.AbstractQuery.ResultIterator.fieldsIndex] */
    realFields.toSet()

fun Query.getFieldExpressionSet() =
    set.getFieldExpressionSet()

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

/**
 * When using this function, it's recommended to name the lambda parameter the same as the outer receiver so that the outer [DatabaseClient] is shadowed,
 * and so that you don't call the outer [DatabaseClient] without a transaction by accident.
 */
suspend fun <T> DatabaseClient<PgPool>.withTransaction(function: suspend (DatabaseClient<SqlConnection>) -> T): T =
    coroutineScope {
        vertxSqlClient.withTransaction {
            coroutineToFuture { function(DatabaseClient(it, exposedDatabase)) }
        }.coAwait()
    }

suspend fun <T> DatabaseClient<PgPool>.withPgTransaction(function: suspend (DatabaseClient<PgConnection>) -> T): T =
    withTransaction {
        @Suppress("UNCHECKED_CAST")
        function(it as DatabaseClient<PgConnection>)
    }

suspend fun <T> DatabaseClient<SqlConnection>.withTransactionCommitOrRollback(function: suspend (DatabaseClient<SqlConnection>) -> Option<T>): Option<T> {
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

private suspend fun DatabaseClient<PgConnection>.savepoint(savepointName: String) =
    executePlainSqlUpdate("SAVEPOINT \"$savepointName\"").also { dbAssert(it == 0) }

private suspend fun DatabaseClient<PgConnection>.rollbackToSavepoint(savepointName: String) =
    executePlainSqlUpdate("ROLLBACK TO SAVEPOINT \"$savepointName\"").also { dbAssert(it == 0) }

private suspend fun DatabaseClient<PgConnection>.releaseSavepoint(savepointName: String) =
    executePlainSqlUpdate("RELEASE SAVEPOINT \"$savepointName\"").also { dbAssert(it == 0) }

/**
 * Currently only available for PostgreSQL.
 * A savepoint destroys one with the same name so be careful.
 */
suspend fun <RollbackT, ReleaseT> DatabaseClient<PgConnection>.withSavepointAndRollbackIfThrowsOrLeft(
    savepointName: String, function: suspend (DatabaseClient<PgConnection>) -> Either<RollbackT, ReleaseT>
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

suspend fun <T> DatabaseClient<PgConnection>.withSavepointAndRollbackIfThrows(
    savepointName: String, function: suspend (DatabaseClient<PgConnection>) -> T
): T =
    withSavepointAndRollbackIfThrowsOrLeft(savepointName) { function(it).right() }.getOrElse { throw AssertionError() }

suspend fun <T> DatabaseClient<PgConnection>.withSavepointAndRollbackIfThrowsOrNone(
    savepointName: String, function: suspend (DatabaseClient<PgConnection>) -> Option<T>
): Option<T> =
    withSavepointAndRollbackIfThrowsOrLeft(savepointName) { function(it).toEither { } }.getOrNone()

suspend fun DatabaseClient<PgConnection>.withSavepointAndRollbackIfThrowsOrFalse(
    savepointName: String, function: suspend (DatabaseClient<PgConnection>) -> Boolean
): Boolean =
    withSavepointAndRollbackIfThrowsOrLeft(savepointName) { if (function(it)) Unit.right() else Unit.left() }.isRight()

// TODO: use `ConnectionConfig` as the argument directly

// can be used for a shared Exposed `Database` among `DatabaseClient`s
fun createPgPoolDatabaseClient(
    vertx: Vertx?,
    vertxSqlClientConnectionConfig: ConnectionConfig,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}, poolOptions: PoolOptions = poolOptionsOf(),
    exposedDatabase: Database
): DatabaseClient<PgPool> =
    DatabaseClient(
        with(vertxSqlClientConnectionConfig) {
            when (this) {
                is Socket ->
                    createSocketPgPool(vertx, host, port, database, user, password, extraPgConnectOptions, poolOptions)

                is UnixDomainSocketWithPeerAuthentication ->
                    createPeerAuthenticationUnixDomainSocketPgPoolAndSetRole(
                        vertx, path, database, role, extraPgConnectOptions, poolOptions
                    )
            }
        },
        exposedDatabase
    )

fun createPgPoolDatabaseClient(
    vertx: Vertx?,
    vertxSqlClientConnectionConfig: ConnectionConfig,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}, poolOptions: PoolOptions = poolOptionsOf(),
    exposedSocketConnectionConfig: Socket
): DatabaseClient<PgPool> =
    createPgPoolDatabaseClient(
        vertx, vertxSqlClientConnectionConfig, extraPgConnectOptions, poolOptions,
        exposedDatabaseConnectPostgreSql(exposedSocketConnectionConfig)
    )

/** It may be more efficient to use a single shared [Database] to generate SQLs for multiple [DatabaseClient]s/[SqlClient]s. */
fun createPgPoolDatabaseClient(
    vertx: Vertx?,
    vertxSqlClientConnectionType: ConnectionType, localConnectionConfig: LocalConnectionConfig,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}, poolOptions: PoolOptions = poolOptionsOf(),
    exposedDatabase: Database? = null
) =
    with(localConnectionConfig) {
        val connectionConfig = when (vertxSqlClientConnectionType) {
            ConnectionType.Socket -> socketConnectionConfig
            ConnectionType.UnixDomainSocketWithPeerAuthentication -> unixDomainSocketWithPeerAuthenticationConnectionConfig
        }

        if (exposedDatabase === null)
            createPgPoolDatabaseClient(
                vertx, connectionConfig, extraPgConnectOptions, poolOptions, socketConnectionConfig
            )
        else
            createPgPoolDatabaseClient(
                vertx, connectionConfig, extraPgConnectOptions, poolOptions, exposedDatabase
            )
    }

fun createBetterPgPoolDatabaseClient(
    vertx: Vertx?,
    localConnectionConfig: LocalConnectionConfig,
    extraPgConnectOptions: PgConnectOptions.() -> Unit = {}, poolOptions: PoolOptions = poolOptionsOf(),
    exposedDatabase: Database? = null
) =
    createPgPoolDatabaseClient(
        vertx,
        if (isOSLinux()) ConnectionType.UnixDomainSocketWithPeerAuthentication else ConnectionType.Socket,
        localConnectionConfig,
        extraPgConnectOptions, poolOptions,
        exposedDatabase
    )
