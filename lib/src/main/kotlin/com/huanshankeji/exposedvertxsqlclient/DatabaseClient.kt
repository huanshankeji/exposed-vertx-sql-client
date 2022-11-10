package com.huanshankeji.exposedvertxsqlclient

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.huanshankeji.exposed.datamapping.DataMapper
import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig.Socket
import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig.UnixDomainSocketWithPeerAuthentication
import com.huanshankeji.exposedvertxsqlclient.classpropertymapping.ClassPropertyIndexReadMapper
import com.huanshankeji.os.isOSLinux
import com.huanshankeji.vertx.kotlin.coroutines.coroutineToFuture
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.kotlin.coroutines.await
import io.vertx.pgclient.PgConnection
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.*
import kotlinx.coroutines.coroutineScope
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.statements.Statement
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.function.Function
import kotlin.reflect.KClass
import org.jetbrains.exposed.sql.Transaction as ExposedTransaction

typealias ExposedArgs = Iterable<Pair<IColumnType, Any?>>

/**
 * A wrapper client around Vert.x [SqlClient] for queries and an Exposed [Database] to generate SQLs working around the limitations of Exposed.
 */
class DatabaseClient<out VertxSqlClient : SqlClient>(
    val vertxSqlClient: VertxSqlClient,
    val exposedDatabase: Database
) {
    suspend fun close() {
        vertxSqlClient.close().await()
        // How to close The Exposed `Database`?
    }

    fun dbAssert(b: Boolean) {
        if (!b)
            throw AssertionError()
    }

    fun <T> exposedTransaction(statement: ExposedTransaction.() -> T) =
        transaction(exposedDatabase, statement)

    suspend fun executePlainSql(sql: String): RowSet<Row> =
        vertxSqlClient.query(sql).execute().await()

    suspend fun executePlainSqlUpdate(sql: String): Int =
        executePlainSql(sql).rowCount()


    fun List<String>.joinSqls(): String =
        joinToString(";\n", postfix = ";")

    /**
     * @see SchemaUtils.create
     */
    @Deprecated(
        "This function does not support analyzing dependencies among tables. Since this action is not frequently needed we can adopt the blocking approach. Use Exposed SchemaUtils and create multiple tables in batch instead, temporarily.",
        ReplaceWith("exposedTransaction { SchemaUtils.create(table) }", "org.jetbrains.exposed.sql.SchemaUtils")
    )
    suspend fun createTable(table: Table) =
        executePlainSqlUpdate(exposedTransaction {
            //table.createStatement()
            (table.ddl + table.indices.flatMap { it.createStatement() }).joinSqls()
        })

    @Deprecated(
        "This function does not support analyzing dependencies among tables. Since this action is not frequently needed we can adopt the blocking approach. Use Exposed SchemaUtils and drop multiple tables in batch instead, temporarily.",
        ReplaceWith("exposedTransaction { SchemaUtils.drop(table) }", "org.jetbrains.exposed.sql.SchemaUtils")
    )
    suspend fun dropTable(table: Table) =
        executePlainSqlUpdate(exposedTransaction {
            table.dropStatement().joinSqls()
        })


    // TODO: context receivers
    fun Statement<*>.getVertxPgClientPreparedSql(transaction: ExposedTransaction) =
        prepareSQL(transaction).toVertxPgClientPreparedSql()

    /**
     * This method has to be called within an [ExposedTransaction].
     */
    fun Statement<*>.getVertxSqlClientArgTuple() =
        arguments().firstOrNull()?.toVertxTuple()

    private suspend inline fun <U> doExecute(
        statement: Statement<*>,
        beforeExecution: PreparedQuery<RowSet<Row>>.() -> PreparedQuery<RowSet<U>>
    ): RowSet<U> {
        val (sql, argTuple) = exposedTransaction {
            statement.getVertxPgClientPreparedSql(this) to
                    statement.getVertxSqlClientArgTuple()
        }
        return vertxSqlClient.preparedQuery(sql)
            .beforeExecution()
            .run { if (argTuple === null) execute() else execute(argTuple) }
            .await()
    }

    fun String.toVertxPgClientPreparedSql(): String {
        val stringBuilder = StringBuilder(length * 2)
        var i = 1
        for (c in this)
            if (c == '?') stringBuilder.append('$').append(i++)
            else stringBuilder.append(c)
        return stringBuilder.toString()
    }

    fun ExposedArgs.toVertxTuple(): Tuple =
        Tuple.wrap(map {
            val value = it.second
            if (value is EntityID<*>) value.value else value
        })

    suspend fun executeForVertxSqlClientRowSet(statement: Statement<*>): RowSet<Row> =
        doExecute(statement) { this }

    suspend fun <U> executeWithMapping(statement: Statement<*>, mapper: Function<Row, U>): RowSet<U> =
        doExecute(statement) { mapping(mapper) }

    fun Row.toExposedResultRow(queryFieldSet: Set<Expression<*>>) =
        ResultRow.createAndFillValues(
            queryFieldSet.asSequence().mapIndexed { index, expression ->
                expression to getValue(index).let {
                    when (it) {
                        is Buffer -> it.bytes
                        else -> it
                    }
                }
            }.toMap()
        )

    fun Query.getFieldSet() =
        /** [org.jetbrains.exposed.sql.AbstractQuery.ResultIterator.fieldsIndex] */
        set.realFields.toSet()

    fun Row.toExposedResultRow(query: Query) =
        toExposedResultRow(query.getFieldSet())

    suspend fun executeQuery(query: Query): RowSet<ResultRow> =
        executeWithMapping(query) { row -> row.toExposedResultRow(query) }

    suspend fun <Data : Any> executeQuery(query: Query, dataMapper: DataMapper<Data>): RowSet<Data> =
        executeWithMapping(query) { row -> dataMapper.resultRowToData(row.toExposedResultRow(query)) }

    suspend fun <Data : Any> executeQuery(
        query: Query, classPropertyIndexReadMapper: ClassPropertyIndexReadMapper<Data>
    ): RowSet<Data> =
        executeWithMapping(query, classPropertyIndexReadMapper::rowToData)

    suspend fun <Data : Any> executeSelectQuery(
        columnSet: ColumnSet, dataMapper: DataMapper<Data>, buildQuery: FieldSet.() -> Query
    ) =
        executeQuery(columnSet.slice(dataMapper.neededColumns).buildQuery(), dataMapper)

    suspend fun executeUpdate(statement: Statement<Int>): Int =
        executeForVertxSqlClientRowSet(statement).rowCount()

    class SingleUpdateException(rowCount: Int) : Exception("update row count: $rowCount")

    suspend fun executeSingleOrNoUpdate(statement: Statement<Int>): Boolean =
        when (val rowCount = executeUpdate(statement)) {
            0 -> false
            1 -> true
            else -> throw SingleUpdateException(rowCount)
        }

    suspend fun executeSingleUpdate(statement: Statement<Int>): Unit =
        require(executeUpdate(statement) == 1)

    // see: https://github.com/JetBrains/Exposed/issues/621
    suspend fun <T : Any> executeExpression(clazz: KClass<T>, expression: Expression<T?>): T? =
        executeForVertxSqlClientRowSet(Table.Dual.slice(expression).selectAll())
            .single()[clazz.java, 0]

    suspend inline fun <reified T> executeExpression(expression: Expression<T>): T =
        @Suppress("UNCHECKED_CAST")
        executeExpression(T::class as KClass<Any>, expression as Expression<Any?>) as T

    suspend fun isWorking(): Boolean =
        try {
            executePlainSql("SELECT TRUE;").first().getBoolean(0)
        } catch (e: IllegalArgumentException) {
            false
        }


    /**
     * @param statement a statement with dummy arguments set that can be mutated with different arguments.
     * @see batchInsert
     * @see PreparedQuery.executeBatch
     */
    suspend fun <U, StatementT : Statement<*>, E> doBatchExecute(
        statement: StatementT,
        data: List<E>,
        setStatementArgs: StatementT.(E) -> Unit,
        beforeExecution: PreparedQuery<RowSet<Row>>.() -> PreparedQuery<RowSet<U>>
    ): RowSet<U> {
        val (sql, argTuples) = exposedTransaction {
            statement.getVertxPgClientPreparedSql(this) to
                    data.map {
                        // The statement is mutable and reused here for all data so the `map` should be parallelized.
                        statement.setStatementArgs(it)
                        statement.getVertxSqlClientArgTuple()
                    }
        }
        return vertxSqlClient.preparedQuery(sql)
            .beforeExecution()
            .executeBatch(argTuples).await()
    }

    suspend fun <StatementT : Statement<*>, E> batchExecuteForVertxSqlClientRowSet(
        statement: StatementT, data: List<E>, setStatementArgs: StatementT.(E) -> Unit
    ): RowSet<Row> =
        doBatchExecute(statement, data, setStatementArgs) { this }

    suspend fun <E> batchExecuteQuery(
        query: Query, data: List<E>, setStatementArgs: Query.(E) -> Unit
    ): RowSet<ResultRow> {
        val queryFieldSet = query.getFieldSet()
        return doBatchExecute(query, data, setStatementArgs) {
            mapping { it.toExposedResultRow(queryFieldSet) }
        }
    }

    suspend fun <E> batchExecuteUpdate(
        statement: UpdateBuilder<Int>, data: List<E>, setStatementArgs: UpdateBuilder<Int>.(E) -> Unit
    ): Int =
        batchExecuteForVertxSqlClientRowSet(statement, data, setStatementArgs).rowCount()
}


fun <R> RowSet<R>.singleResult(): R =
    single()

/** "single or no" means differently here from [Iterable.singleOrNull]. */
fun <R> RowSet<R>.singleOrNoResult(): R? =
    if (none()) null else single()


suspend fun <T> DatabaseClient<PgPool>.withTransaction(function: suspend (DatabaseClient<SqlConnection>) -> T): T =
    coroutineScope {
        vertxSqlClient.withTransaction {
            coroutineToFuture { function(DatabaseClient(it, exposedDatabase)) }
        }.await()
    }

suspend fun <T> DatabaseClient<PgPool>.withPgTransaction(function: suspend (DatabaseClient<PgConnection>) -> T): T =
    withTransaction {
        @Suppress("UNCHECKED_CAST")
        function(it as DatabaseClient<PgConnection>)
    }

suspend fun <T> DatabaseClient<SqlConnection>.withTransactionCommitOrRollback(function: suspend (DatabaseClient<SqlConnection>) -> Option<T>): Option<T> {
    val transaction = vertxSqlClient.begin().await()
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

// for PostgreSQL
// A savepoint destroys one with the same name so be careful.
suspend fun <T> DatabaseClient<PgConnection>.withSavepointMayRollbackToIt(
    savepointName: String, function: suspend (DatabaseClient<PgConnection>) -> Option<T>
): Option<T> {
    // Prepared query seems not to work here.

    require(savepointName.matches(savepointNameRegex))
    executePlainSqlUpdate("SAVEPOINT \"$savepointName\"").also { dbAssert(it == 0) }

    suspend fun rollbackToSavepoint() =
        executePlainSqlUpdate("ROLLBACK TO SAVEPOINT \"$savepointName\"").also { dbAssert(it == 0) }

    return try {
        val result = function(this)
        if (result.isEmpty()) rollbackToSavepoint()
        else executePlainSqlUpdate("RELEASE SAVEPOINT \"$savepointName\"").also { dbAssert(it == 0) }
        result
    } catch (e: Exception) {
        rollbackToSavepoint()
        throw e
    }
}

enum class ConnectionType {
    Socket, UnixDomainSocketWithPeerAuthentication
}

sealed interface ConnectionConfig {
    val userAndRole: String
    val database: String

    class Socket(
        val host: String,
        val user: String,
        val password: String,
        override val database: String
    ) : ConnectionConfig {
        override val userAndRole: String get() = user
    }

    class UnixDomainSocketWithPeerAuthentication(
        val path: String,
        val role: String,
        override val database: String
    ) : ConnectionConfig {
        override val userAndRole: String get() = role
    }
}

// can be used for a shared Exposed `Database` among `DatabaseClient`s
fun createDatabaseClient(
    vertx: Vertx? = null,
    vertxSqlClientConnectionConfig: ConnectionConfig, exposedDatabase: Database
): DatabaseClient<PgPool> =
    DatabaseClient(
        with(vertxSqlClientConnectionConfig) {
            when (this) {
                is Socket -> createSocketPgPool(vertx, host, database, user, password)
                is UnixDomainSocketWithPeerAuthentication ->
                    createPeerAuthenticationUnixDomainSocketPgPoolAndSetRole(vertx, path, database, role)
            }
        },
        exposedDatabase
    )

fun createDatabaseClient(
    vertx: Vertx? = null,
    vertxSqlClientConnectionConfig: ConnectionConfig, exposedSocketConnectionConfig: Socket
): DatabaseClient<PgPool> =
    createDatabaseClient(
        vertx, vertxSqlClientConnectionConfig,
        exposedDatabaseConnectPostgreSql(exposedSocketConnectionConfig)
    )

fun createDatabaseClient(
    vertx: Vertx? = null,
    vertxSqlClientConnectionType: ConnectionType, config: Config,
    exposedDatabase: Database? = null
) =
    with(config) {
        val connectionConfig = when (vertxSqlClientConnectionType) {
            ConnectionType.Socket -> socketConnectionConfig
            ConnectionType.UnixDomainSocketWithPeerAuthentication -> unixDomainSocketWithPeerAuthenticationConnectionConfig
        }

        if (exposedDatabase === null)
            createDatabaseClient(vertx, connectionConfig, socketConnectionConfig)
        else
            createDatabaseClient(vertx, connectionConfig, exposedDatabase)
    }

fun createBetterDatabaseClient(
    vertx: Vertx? = null,
    config: Config,
    exposedDatabase: Database? = null
) =
    createDatabaseClient(
        vertx,
        if (isOSLinux()) ConnectionType.UnixDomainSocketWithPeerAuthentication else ConnectionType.Socket,
        config,
        exposedDatabase
    )
