@file:OptIn(ExperimentalEvscApi::class)

package com.huanshankeji.exposedvertxsqlclient.sql

import com.huanshankeji.exposed.*
import com.huanshankeji.exposedvertxsqlclient.DatabaseClient
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.dbAssert
import com.huanshankeji.exposedvertxsqlclient.singleOrNoUpdate
import com.huanshankeji.vertx.sqlclient.sortDataAndExecuteBatch
import io.vertx.sqlclient.RowSet
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertSelectStatement
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import kotlin.reflect.KClass
import kotlin.sequences.Sequence

/* TODO Consider moving these to a separate non-compulsory module because there are too many kinds of different [Statement]s.
    Supporting all of them may be difficult and pose an extra cognitive burden on a user. */

suspend inline fun <Data> DatabaseClient<*>.select(
    columnSet: ColumnSet, buildQuery: ColumnSet.() -> Query, crossinline resultRowMapper: ResultRow.() -> Data
): RowSet<Data> =
    executeQuery(columnSet.buildQuery(), resultRowMapper)

suspend inline fun DatabaseClient<*>.select(
    columnSet: ColumnSet, buildQuery: ColumnSet.() -> Query
): RowSet<ResultRow> =
    @Suppress("MoveLambdaOutsideParentheses")
    select(columnSet, buildQuery, { this })


/**
 * SQL: `SELECT <expression> FROM <table>;`.
 * Examples: `SELECT COUNT(*) FROM <table>;`, `SELECT SUM(<column>) FROM <table>;`.
 */
@ExperimentalEvscApi
suspend fun <T> DatabaseClient<*>.selectColumnSetExpression(
    columnSet: ColumnSet, expression: Expression<T>, buildQuery: Query.() -> Query
): RowSet<T> =
    select(columnSet, { select(expression).buildQuery() }, { this[expression] })

// This function with `mapper` is not really useful
@ExperimentalEvscApi
suspend inline fun <ColumnT, DataT> DatabaseClient<*>.selectSingleColumn(
    columnSet: ColumnSet,
    column: Column<ColumnT>,
    buildQuery: Query.() -> Query,
    crossinline mapper: ColumnT.() -> DataT
): RowSet<DataT> =
    select(columnSet, { select(column).buildQuery() }, { this[column].mapper() })

suspend fun <T> DatabaseClient<*>.selectSingleColumn(
    columnSet: ColumnSet, column: Column<T>, buildQuery: Query.() -> Query
): RowSet<T> =
    selectColumnSetExpression(columnSet, column, buildQuery)

suspend fun <T : Comparable<T>> DatabaseClient<*>.selectSingleEntityIdColumn(
    columnSet: ColumnSet, column: Column<EntityID<T>>, buildQuery: Query.() -> Query
): RowSet<T> =
    selectSingleColumn(columnSet, column, buildQuery) { value }


/**
 * SQL: `SELECT <expression>;`.
 * Example: `SELECT EXISTS(<query>)`.
 */
// see: https://github.com/JetBrains/Exposed/issues/621
suspend fun <T : Any> DatabaseClient<*>.selectExpression(clazz: KClass<T>, expression: Expression<T?>): T? =
    executeForVertxSqlClientRowSet(Table.Dual.select(expression))
        .single()[clazz.java, 0]

suspend inline fun <reified T> DatabaseClient<*>.selectExpression(expression: Expression<T>): T =
    @Suppress("UNCHECKED_CAST")
    (selectExpression(T::class as KClass<Any>, expression as Expression<Any?>)) as T

@ExperimentalEvscApi
suspend fun <T : Table> DatabaseClient<*>.insert(table: T, body: T.(InsertStatement<Number>) -> Unit) =
    executeSingleUpdate(table.insertStatement(body))

@ExperimentalEvscApi
@Deprecated("Use `insert`", ReplaceWith("this.insert<T>(table, body)"))
suspend fun <T : Table> DatabaseClient<*>.insertSingle(table: T, body: T.(InsertStatement<Number>) -> Unit) =
    insert(table, body)

@ExperimentalEvscApi
suspend fun <T : Table> DatabaseClient<*>.insertIgnore(
    table: T, body: T.(InsertStatement<Number>) -> Unit
): Boolean =
    executeSingleOrNoUpdate(table.insertIgnoreStatement(body))

@ExperimentalEvscApi
@Deprecated("Use `insertIgnore`", ReplaceWith("this.insertIgnore<T>(table, body)"))
suspend fun <T : Table> DatabaseClient<*>.insertIgnoreSingle(
    table: T, body: T.(InsertStatement<Number>) -> Unit
): Boolean =
    insertIgnore(table, body)

@ExperimentalEvscApi
@Deprecated("Use `insertIgnore`", ReplaceWith("this.insertIgnore<T>(table, body)"))
suspend fun <T : Table> DatabaseClient<*>.executeInsertIgnore(
    table: T, body: T.(InsertStatement<Number>) -> Unit
): Boolean =
    insertIgnore(table, body)


suspend fun <T : Table> DatabaseClient<*>.insertSelect(
    table: T,
    selectQuery: AbstractQuery<*>,
    columns: List<Column<*>> = table.defaultColumnsForInsertSelect()
) =
    executeUpdate(table.insertSelectStatement(selectQuery, columns))

suspend fun <T : Table> DatabaseClient<*>.insertIgnoreSelect(
    table: T, selectQuery: AbstractQuery<*>, columns: List<Column<*>> = table.defaultColumnsForInsertSelect()
) =
    executeUpdate(table.insertSelectStatement(selectQuery, columns, true))


suspend fun <T : Table> DatabaseClient<*>.update(
    table: T, where: BuildWhere? = null, limit: Int? = null, body: T.(UpdateStatement) -> Unit
) =
    executeUpdate(table.updateStatement(where, limit, body))


/**
 * This function may be very rarely used, as `eq` conditions can usually be combined into an `inList` select query.
 */
@ExperimentalEvscApi
suspend fun <E> DatabaseClient<*>.selectBatch(
    fieldSet: FieldSet, buildQuery: FieldSet.(E) -> Query, data: Iterable<E>
): Sequence<RowSet<ResultRow>> =
    executeBatchQuery(fieldSet, data.asSequence().map { fieldSet.buildQuery(it) }.asIterable())

/**
 * @see DatabaseClient.executeBatchUpdate
 */
suspend fun <T : Table, E> DatabaseClient<*>.batchInsert(
    table: T, data: Iterable<E>, body: T.(InsertStatement<Number>, E) -> Unit
) =
    executeBatchUpdate(data.asSequence().map { element ->
        table.insertStatement { body(it, element) }
    }.asIterable())
        .forEach { dbAssert(it == 1) }

/**
 * @see DatabaseClient.executeBatchUpdate
 */
suspend fun <T : Table, E> DatabaseClient<*>.batchInsertIgnore(
    table: T, data: Iterable<E>, body: T.(InsertStatement<Number>, E) -> Unit
) =
    executeBatchUpdate(data.asSequence().map { element ->
        table.insertIgnoreStatement { body(it, element) }
    }.asIterable())
        .map { it.singleOrNoUpdate() }


/**
 * This function is not conventional and it usages are likely to degrade performance.
 * @see DatabaseClient.executeBatchUpdate
 */
@ExperimentalEvscApi
suspend fun DatabaseClient<*>.batchInsertSelect(
    statements: Iterable<InsertSelectStatement>
) =
    executeBatchUpdate(statements)

/**
 * @see DatabaseClient.executeBatchUpdate
 * @see sortDataAndBatchUpdate
 */
suspend fun <T : Table, E> DatabaseClient<*>.batchUpdate(
    table: T, data: Iterable<E>, where: BuildWhere? = null, limit: Int? = null, body: T.(UpdateStatement, E) -> Unit
) =
    executeBatchUpdate(data.asSequence().map { element ->
        table.updateStatement(where, limit) { body(it, element) }
    }.asIterable())

/**
 * @return a sequence indicating whether each update statement is updated in the batch.
 * @see batchUpdate
 */
suspend fun <T : Table, E> DatabaseClient<*>.batchSingleOrNoUpdate(
    table: T, data: Iterable<E>, where: BuildWhere? = null, limit: Int? = null, body: T.(UpdateStatement, E) -> Unit
): Sequence<Boolean> =
    batchUpdate(table, data, where, limit, body).map { it.singleOrNoUpdate() }

/**
 * @see sortDataAndExecuteBatch
 * @see batchUpdate
 */
suspend fun <T : Table, E, SelectorResultT : Comparable<SelectorResultT>> DatabaseClient<*>.sortDataAndBatchUpdate(
    table: T,
    data: Iterable<E>, selector: (E) -> SelectorResultT,
    where: BuildWhere? = null, limit: Int? = null, body: T.(UpdateStatement, E) -> Unit
) =
    batchUpdate(table, data.sortedBy(selector), where, limit, body)


suspend fun <T : Table> DatabaseClient<*>.deleteWhere(
    table: T, limit: Int? = null, offset: Long? = null, op: TableAwareWithSqlExpressionBuilderBuildWhere<T>
) =
    executeUpdate(table.deleteWhereStatement(limit, offset, op))

suspend fun <T : Table> DatabaseClient<*>.deleteIgnoreWhere(
    table: T, limit: Int? = null, offset: Long? = null, op: TableAwareWithSqlExpressionBuilderBuildWhere<T>
) =
    executeUpdate(table.deleteIgnoreWhereStatement(limit, offset, op))
