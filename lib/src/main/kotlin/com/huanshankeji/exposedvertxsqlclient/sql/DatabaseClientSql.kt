@file:OptIn(ExperimentalEvscApi::class)

package com.huanshankeji.exposedvertxsqlclient.sql

import com.huanshankeji.exposed.*
import com.huanshankeji.exposedvertxsqlclient.*
import com.huanshankeji.vertx.sqlclient.sortDataAndExecuteBatch
import io.vertx.sqlclient.RowSet
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertSelectStatement
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import kotlin.reflect.KClass
import kotlin.sequences.Sequence

// This function with `mapper` is not really useful
@ExperimentalEvscApi
suspend inline fun <T, R> DatabaseClient<*>.selectSingleColumn(
    columnSet: ColumnSet, column: Column<T>, buildQuery: FieldSet.() -> Query, crossinline mapper: T.() -> R
): RowSet<R> =
    executeQuery(columnSet.slice(column).buildQuery()) { this[column].mapper() }


@Deprecated("Use `selectSingleColumn`.", ReplaceWith("selectSingleColumn<T, R>(columnSet, column, buildQuery, mapper)"))
@ExperimentalEvscApi
suspend inline fun <T, R> DatabaseClient<*>.executeSingleColumnSelectQuery(
    columnSet: ColumnSet, column: Column<T>, buildQuery: FieldSet.() -> Query, crossinline mapper: T.() -> R
): RowSet<R> =
    selectSingleColumn(columnSet, column, buildQuery, mapper)

suspend fun <T> DatabaseClient<*>.selectSingleColumn(
    columnSet: ColumnSet, column: Column<T>, buildQuery: FieldSet.() -> Query
): RowSet<T> =
    selectSingleColumn(columnSet, column, buildQuery) { this }

@Deprecated("Use `selectSingleColumn`.", ReplaceWith("selectSingleColumn<T>(columnSet, column, buildQuery)"))
suspend fun <T> DatabaseClient<*>.executeSingleColumnSelectQuery(
    columnSet: ColumnSet, column: Column<T>, buildQuery: FieldSet.() -> Query
): RowSet<T> =
    selectSingleColumn(columnSet, column, buildQuery)

suspend fun <T : Comparable<T>> DatabaseClient<*>.selectSingleEntityIdColumn(
    columnSet: ColumnSet, column: Column<EntityID<T>>, buildQuery: FieldSet.() -> Query
): RowSet<T> =
    selectSingleColumn(columnSet, column, buildQuery) { value }


// see: https://github.com/JetBrains/Exposed/issues/621
suspend fun <T : Any> DatabaseClient<*>.selectExpression(clazz: KClass<T>, expression: Expression<T?>): T? =
    executeForVertxSqlClientRowSet(Table.Dual.slice(expression).selectAll())
        .single()[clazz.java, 0]

suspend inline fun <reified T> DatabaseClient<*>.selectExpression(expression: Expression<T>): T =
    @Suppress("UNCHECKED_CAST")
    (selectExpression(T::class as KClass<Any>, expression as Expression<Any?>)) as T


@ExperimentalEvscApi
suspend fun <T : Table> DatabaseClient<*>.insertSingle(table: T, body: T.(InsertStatement<Number>) -> Unit) =
    executeSingleUpdate(table.insertStatement(body))

@ExperimentalEvscApi
suspend fun <T : Table> DatabaseClient<*>.insertIgnoreSingle(
    table: T,
    body: T.(InsertStatement<Number>) -> Unit
): Boolean =
    executeSingleOrNoUpdate(table.insertIgnoreStatement(body))

@ExperimentalEvscApi
@Deprecated("Use `insertIgnoreSingle`", ReplaceWith("this.insertIgnoreSingle<T>(table, body)"))
suspend fun <T : Table> DatabaseClient<*>.executeInsertIgnore(
    table: T,
    body: T.(InsertStatement<Number>) -> Unit
): Boolean =
    insertIgnoreSingle(table, body)


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
): Sequence<RowSet<ResultRow>> {
    val fieldExpressionSet = fieldSet.getFieldExpressionSet()
    return doExecuteBatchCreatingStatementForEachElement({ fieldSet.buildQuery(it) }, data) {
        mapping { it.toExposedResultRow(fieldExpressionSet) }
    }
}


suspend fun <T : Table, E> DatabaseClient<*>.batchInsert(
    table: T, data: Iterable<E>, body: T.(InsertStatement<Number>, E) -> Unit
) =
    executeBatchUpdate(data.asSequence().map { element ->
        table.insertStatement { body(it, element) }
    }.asIterable())
        .forEach { dbAssert(it == 1) }

suspend fun <T : Table, E> DatabaseClient<*>.batchInsertIgnore(
    table: T, data: Iterable<E>, body: T.(InsertStatement<Number>, E) -> Unit
) =
    executeBatchUpdate(data.asSequence().map { element ->
        table.insertIgnoreStatement { body(it, element) }
    }.asIterable())


/**
 * This function is not conventional and it usages are likely to degrade performance.
 */
@ExperimentalEvscApi
suspend fun DatabaseClient<*>.batchInsertSelect(
    statements: Iterable<InsertSelectStatement>
) =
    executeBatchUpdate(statements)

suspend fun <T : Table, E> DatabaseClient<*>.batchUpdate(
    table: T, data: Iterable<E>, where: BuildWhere? = null, limit: Int? = null, body: T.(UpdateStatement, E) -> Unit
) =
    executeBatchUpdate(data.asSequence().map { element ->
        table.updateStatement(where, limit) { body(it, element) }
    }.asIterable())

/**
 * @return a sequence indicating whether each update statement is updated in the batch.
 */
suspend fun <T : Table, E> DatabaseClient<*>.batchSingleOrNoUpdate(
    table: T, data: Iterable<E>, where: BuildWhere? = null, limit: Int? = null, body: T.(UpdateStatement, E) -> Unit
): Sequence<Boolean> =
    batchUpdate(table, data, where, limit, body).map { it.singleOrNoUpdate() }

/**
 * @see sortDataAndExecuteBatch
 */
suspend fun <T : Table, E, SelectorResultT : Comparable<SelectorResultT>> DatabaseClient<*>.sortDataAndBatchUpdate(
    table: T,
    data: Iterable<E>, selector: (E) -> SelectorResultT,
    where: BuildWhere? = null, limit: Int? = null, body: T.(UpdateStatement, E) -> Unit
) =
    batchUpdate(table, data.sortedBy(selector), where, limit, body)
