@file:OptIn(ExperimentalEvscApi::class)

package com.huanshankeji.exposedvertxsqlclient.crud

import com.huanshankeji.exposedvertxsqlclient.DatabaseClient
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.dbAssert
import com.huanshankeji.exposedvertxsqlclient.singleOrNoUpdate
import com.huanshankeji.vertx.sqlclient.sortDataAndExecuteBatch
import io.vertx.sqlclient.RowSet
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.statements.*
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.select
import kotlin.reflect.KClass
import kotlin.sequences.Sequence

suspend inline fun <Data> DatabaseClient<*>.select(
    columnSet: ColumnSet,
    buildQuery: ColumnSet.() -> Query,
    getFieldExpressionSetWithExposedTransaction: Boolean = config.autoExposedTransaction,
    crossinline resultRowMapper: ResultRow.() -> Data
): RowSet<Data> =
    executeQuery(columnSet.buildQuery(), getFieldExpressionSetWithExposedTransaction, resultRowMapper)

suspend inline fun DatabaseClient<*>.select(
    columnSet: ColumnSet,
    buildQuery: ColumnSet.() -> Query,
    getFieldExpressionSetWithExposedTransaction: Boolean = config.autoExposedTransaction
): RowSet<ResultRow> =
    @Suppress("MoveLambdaOutsideParentheses")
    select(columnSet, buildQuery, getFieldExpressionSetWithExposedTransaction, { this })


/**
 * SQL: `SELECT <expression> FROM <table>;`.
 * Examples: `SELECT COUNT(*) FROM <table>;`, `SELECT SUM(<column>) FROM <table>;`.
 */
@ExperimentalEvscApi
suspend fun <T> DatabaseClient<*>.selectColumnSetExpression(
    columnSet: ColumnSet,
    expression: Expression<T>,
    buildQuery: Query.() -> Query,
    getFieldExpressionSetWithExposedTransaction: Boolean = config.autoExposedTransaction
): RowSet<T> =
    select(
        columnSet,
        { select(expression).buildQuery() },
        getFieldExpressionSetWithExposedTransaction,
        { this[expression] })

// This function with `mapper` is not really useful
@ExperimentalEvscApi
suspend inline fun <ColumnT, DataT> DatabaseClient<*>.selectSingleColumn(
    columnSet: ColumnSet,
    column: Column<ColumnT>,
    buildQuery: Query.() -> Query,
    getFieldExpressionSetWithExposedTransaction: Boolean = config.autoExposedTransaction,
    crossinline mapper: ColumnT.() -> DataT
): RowSet<DataT> =
    select(
        columnSet,
        { select(column).buildQuery() },
        getFieldExpressionSetWithExposedTransaction,
        { this[column].mapper() })

suspend fun <T> DatabaseClient<*>.selectSingleColumn(
    columnSet: ColumnSet, column: Column<T>, buildQuery: Query.() -> Query,
    getFieldExpressionSetWithExposedTransaction: Boolean = config.autoExposedTransaction
): RowSet<T> =
    selectColumnSetExpression(columnSet, column, buildQuery, getFieldExpressionSetWithExposedTransaction)

suspend fun <T : Comparable<T>> DatabaseClient<*>.selectSingleEntityIdColumn(
    columnSet: ColumnSet,
    column: Column<EntityID<T>>,
    buildQuery: Query.() -> Query,
    getFieldExpressionSetWithExposedTransaction: Boolean = config.autoExposedTransaction
): RowSet<T> =
    selectSingleColumn(columnSet, column, buildQuery, getFieldExpressionSetWithExposedTransaction) { value }


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
    selectExpression(T::class as KClass<Any>, expression as Expression<Any?>) as T

@ExperimentalEvscApi
suspend fun <T : Table> DatabaseClient<*>.insert(table: T, body: T.(InsertStatement<Number>) -> Unit) =
    executeSingleUpdate(buildStatement { table.insert(body) })

@ExperimentalEvscApi
@Deprecated("Use `insert`", ReplaceWith("this.insert<T>(table, body)"))
suspend fun <T : Table> DatabaseClient<*>.insertSingle(table: T, body: T.(InsertStatement<Number>) -> Unit) =
    insert(table, body)

@ExperimentalEvscApi
suspend fun <T : Table> DatabaseClient<*>.insertIgnore(
    table: T, body: T.(UpdateBuilder<*>) -> Unit
): Boolean =
    executeSingleOrNoUpdate(buildStatement { table.insertIgnore(body) })

@ExperimentalEvscApi
@Deprecated("Use `insertIgnore`", ReplaceWith("this.insertIgnore<T>(table, body)"))
suspend fun <T : Table> DatabaseClient<*>.insertIgnoreSingle(
    table: T, body: T.(UpdateBuilder<*>) -> Unit
): Boolean =
    insertIgnore(table, body)

@ExperimentalEvscApi
@Deprecated("Use `insertIgnore`", ReplaceWith("this.insertIgnore<T>(table, body)"))
suspend fun <T : Table> DatabaseClient<*>.executeInsertIgnore(
    table: T, body: T.(UpdateBuilder<*>) -> Unit
): Boolean =
    insertIgnore(table, body)


suspend fun <T : Table> DatabaseClient<*>.insertSelect(
    table: T, selectQuery: AbstractQuery<*>, columns: List<Column<*>>? = null
) = executeUpdate(buildStatement { table.insert(selectQuery, columns) })

suspend fun <T : Table> DatabaseClient<*>.insertIgnoreSelect(
    table: T, selectQuery: AbstractQuery<*>, columns: List<Column<*>>? = null
) = executeUpdate(buildStatement { table.insertIgnore(selectQuery, columns) })


suspend fun <T : Table> DatabaseClient<*>.update(
    table: T, where: (() -> Op<Boolean>)? = null, limit: Int? = null, body: T.(UpdateStatement) -> Unit
) =
    executeUpdate(buildStatement { table.update(where, limit, body) })


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
        buildStatement { table.insert { body(it, element) } }
    }.asIterable())
        .forEach { dbAssert(it == 1) }

/**
 * @see DatabaseClient.executeBatchUpdate
 */
suspend fun <T : Table, E> DatabaseClient<*>.batchInsertIgnore(
    table: T, data: Iterable<E>, body: T.(UpdateBuilder<*>, E) -> Unit
) =
    executeBatchUpdate(data.asSequence().map { element ->
        buildStatement { table.insertIgnore { body(it, element) } }
    }.asIterable())
        .map { it.singleOrNoUpdate() }


/**
 * This function is not conventional and its usages are likely to degrade performance.
 * @see DatabaseClient.executeBatchUpdate
 */
@ExperimentalEvscApi
suspend fun DatabaseClient<*>.batchInsertSelect(statements: Iterable<InsertSelectStatement>) =
    executeBatchUpdate(statements)

/**
 * @see DatabaseClient.executeBatchUpdate
 * @see sortDataAndBatchUpdate
 */
suspend fun <T : Table, E> DatabaseClient<*>.batchUpdate(
    table: T,
    data: Iterable<E>,
    where: (() -> Op<Boolean>)? = null,
    limit: Int? = null,
    body: T.(UpdateStatement, E) -> Unit
) =
    executeBatchUpdate(data.asSequence().map { element ->
        buildStatement { table.update(where, limit) { body(it, element) } }
    }.asIterable())

/**
 * @return a sequence indicating whether each update statement is updated in the batch.
 * @see batchUpdate
 */
suspend fun <T : Table, E> DatabaseClient<*>.batchSingleOrNoUpdate(
    table: T,
    data: Iterable<E>,
    where: (() -> Op<Boolean>)? = null,
    limit: Int? = null,
    body: T.(UpdateStatement, E) -> Unit
): Sequence<Boolean> =
    batchUpdate(table, data, where, limit, body).map { it.singleOrNoUpdate() }

/**
 * @see sortDataAndExecuteBatch
 * @see batchUpdate
 */
suspend fun <T : Table, E, SelectorResultT : Comparable<SelectorResultT>> DatabaseClient<*>.sortDataAndBatchUpdate(
    table: T,
    data: Iterable<E>, selector: (E) -> SelectorResultT,
    where: (() -> Op<Boolean>)? = null, limit: Int? = null, body: T.(UpdateStatement, E) -> Unit
) =
    batchUpdate(table, data.sortedBy(selector), where, limit, body)


suspend fun <T : Table> DatabaseClient<*>.deleteWhere(
    table: T, limit: Int? = null, op: T.() -> Op<Boolean>
) =
    executeUpdate(buildStatement { table.deleteWhere(limit, op) })

@Deprecated("The `offset` parameter is removed.", ReplaceWith("this.deleteWhere(table, limit, op)"))
suspend fun <T : Table> DatabaseClient<*>.deleteWhere(
    table: T, limit: Int? = null, offset: Long? = null, op: T.() -> Op<Boolean>
) =
    deleteWhere(table, limit, op)

suspend fun <T : Table> DatabaseClient<*>.deleteIgnoreWhere(
    table: T, limit: Int? = null, op: T.() -> Op<Boolean>
) =
    executeUpdate(buildStatement { table.deleteIgnoreWhere(limit, op) })

@Deprecated("The `offset` parameter is removed.", ReplaceWith("this.deleteIgnoreWhere(table, limit, op)"))
suspend fun <T : Table> DatabaseClient<*>.deleteIgnoreWhere(
    table: T, limit: Int? = null, offset: Long? = null, op: T.() -> Op<Boolean>
) =
    deleteIgnoreWhere(table, limit, op)
