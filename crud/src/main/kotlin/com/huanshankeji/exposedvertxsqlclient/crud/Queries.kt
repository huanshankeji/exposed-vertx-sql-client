@file:OptIn(ExperimentalEvscApi::class)

package com.huanshankeji.exposedvertxsqlclient.crud

import com.huanshankeji.exposedvertxsqlclient.DatabaseClient
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
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

// CRUD DSL functions akin to those in Exposed
// The function types should be kept consistent with those in Exposed. Corresponding functions are linked in the `@see` annotations in KDoc comments.

// consider making these 2 deprecated `select` functions internal instead of removing them when it's about to remove the deprecated APIs

internal const val SELECT_OVERDESIGN_DEPRECATION_MESSAGE =
    "This API was an over-design, exerts additional cognitive burdens on the user, and has become more redundant with the new Exposed SELECT DSL design. " +
            "Please use `executeQuery` directly."

@Deprecated(
    SELECT_OVERDESIGN_DEPRECATION_MESSAGE,
    ReplaceWith("this.executeQuery(columnSet.buildQuery(), getFieldExpressionSetWithExposedTransaction, resultRowMapper)")
)
suspend inline fun <Data> DatabaseClient<*>.select(
    columnSet: ColumnSet,
    buildQuery: ColumnSet.() -> Query,
    getFieldExpressionSetWithExposedTransaction: Boolean = config.autoExposedTransaction,
    crossinline resultRowMapper: ResultRow.() -> Data
): RowSet<Data> =
    executeQuery(columnSet.buildQuery(), getFieldExpressionSetWithExposedTransaction, resultRowMapper)

@Deprecated(
    SELECT_OVERDESIGN_DEPRECATION_MESSAGE,
    ReplaceWith("this.executeQuery(columnSet.buildQuery(), getFieldExpressionSetWithExposedTransaction)")
)
suspend inline fun DatabaseClient<*>.select(
    columnSet: ColumnSet,
    buildQuery: ColumnSet.() -> Query,
    getFieldExpressionSetWithExposedTransaction: Boolean = config.autoExposedTransaction
): RowSet<ResultRow> =
    @Suppress("MoveLambdaOutsideParentheses", "DEPRECATION")
    select(columnSet, buildQuery, getFieldExpressionSetWithExposedTransaction, { this })


/**
 * SQL: `SELECT <expression> FROM <table>;`.
 * Examples: `SELECT COUNT(*) FROM <table>;`, `SELECT SUM(<column>) FROM <table>;`.
 *
 * This function distinguishes from the overload without a [columnSet] parameter
 * in that it selects from a certain [ColumnSet], which may be a [Table] or a [Join].
 *
 * Also see https://github.com/JetBrains/Exposed/issues/621.
 */
@Deprecated(
    "Use the overload without the `getFieldExpressionSetWithExposedTransaction` parameter, which is implemented more efficiently.",
    ReplaceWith("this.selectExpression(columnSet, expression, buildQuery)")
)
@ExperimentalEvscApi
suspend fun <T> DatabaseClient<*>.selectExpression(
    columnSet: ColumnSet,
    expression: Expression<T>,
    buildQuery: Query.() -> Query,
    getFieldExpressionSetWithExposedTransaction: Boolean /*= config.autoExposedTransaction*/
): RowSet<T> =
    @Suppress("DEPRECATION")
    select(
        columnSet,
        { select(expression).buildQuery() },
        getFieldExpressionSetWithExposedTransaction,
        { this[expression] })

/**
 * SQL: `SELECT <expression> FROM <table>;`.
 * Examples: `SELECT COUNT(*) FROM <table>;`, `SELECT SUM(<column>) FROM <table>;`.
 *
 * This function distinguishes from the overload without a [columnSet] parameter
 * in that it selects from a certain [ColumnSet], which may be a [Table] or a [Join].
 *
 * Also see https://github.com/JetBrains/Exposed/issues/621.
 */
@ExperimentalEvscApi
suspend fun <T> DatabaseClient<*>.selectExpression(
    columnSet: ColumnSet,
    expression: Expression<T>,
    buildQuery: Query.() -> Query
): RowSet<T> =
    @Suppress("DEPRECATION")
    execute(columnSet.select(expression).buildQuery()) {
        mapping {
            // not using the `Row.get` taking a `Class` parameter here, in contrast to the overload without `columnSet`
            @Suppress("UNCHECKED_CAST")
            it.getValue(0) as T
        }
    }

@Deprecated(
    "Renamed to `selectExpression`.",
    ReplaceWith("this.selectExpression(columnSet, expression, buildQuery)")
)
@ExperimentalEvscApi
suspend fun <T> DatabaseClient<*>.selectColumnSetExpression(
    columnSet: ColumnSet,
    expression: Expression<T>,
    buildQuery: Query.() -> Query,
    getFieldExpressionSetWithExposedTransaction: Boolean = config.autoExposedTransaction
): RowSet<T> =
    selectExpression(columnSet, expression, buildQuery, getFieldExpressionSetWithExposedTransaction)

@Deprecated(
    "Use the overload without `mapper` and use Kotlin's `map` instead.",
    ReplaceWith("this.selectSingleColumn(columnSet, column, buildQuery).asSequence().map(mapper)")
)
// This function with `mapper` is not really useful
@ExperimentalEvscApi
suspend inline fun <ColumnT, DataT> DatabaseClient<*>.selectSingleColumn(
    columnSet: ColumnSet,
    column: Column<ColumnT>,
    buildQuery: Query.() -> Query,
    getFieldExpressionSetWithExposedTransaction: Boolean = config.autoExposedTransaction,
    crossinline mapper: ColumnT.() -> DataT
): RowSet<DataT> =
    @Suppress("DEPRECATION")
    select(
        columnSet,
        { select(column).buildQuery() },
        getFieldExpressionSetWithExposedTransaction,
        { this[column].mapper() })

@Deprecated(
    "Use `selectExpression` directly instead.",
    ReplaceWith("this.selectExpression(columnSet, column, buildQuery, getFieldExpressionSetWithExposedTransaction)")
)
suspend fun <T> DatabaseClient<*>.selectSingleColumn(
    columnSet: ColumnSet, column: Column<T>, buildQuery: Query.() -> Query,
    getFieldExpressionSetWithExposedTransaction: Boolean = config.autoExposedTransaction
): RowSet<T> =
    selectExpression(columnSet, column, buildQuery, getFieldExpressionSetWithExposedTransaction)

suspend fun <T : Comparable<T>> DatabaseClient<*>.selectSingleEntityIdColumn(
    columnSet: ColumnSet,
    column: Column<EntityID<T>>,
    buildQuery: Query.() -> Query,
    getFieldExpressionSetWithExposedTransaction: Boolean = config.autoExposedTransaction
): RowSet<T> =
    selectSingleColumn(columnSet, column, buildQuery, getFieldExpressionSetWithExposedTransaction) { value }

// consider porting these 2 functions' implementations to `kotlin-common-vertx`

/**
 * SQL: `SELECT <expression>;` without `FROM` in the outermost/top-level statement.
 * Example: `SELECT EXISTS(<query>)`.
 *
 * Also see https://github.com/JetBrains/Exposed/issues/621.
 *
 * I can't think of a case where this function should return multiple results in a [RowSet]. If there are any in your use cases, please submit an issue.
 */
@ExperimentalEvscApi
suspend fun <T> DatabaseClient<*>.selectExpression(clazz: KClass<T & Any>, expression: Expression<T>): T =
    executeForVertxSqlClientRowSet(Table.Dual.select(expression))
        .single()[clazz.java, 0]

/**
 * SQL: `SELECT <expression>;` without `FROM` in the outermost/top-level statement.
 * Example: `SELECT EXISTS(<query>)`.
 *
 * I can't think of a case where this function should return multiple results in a [RowSet]. If there are any in your use cases, please submit an issue.
 */
@ExperimentalEvscApi
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


/**
 * @see StatementBuilder.insert the overload with `selectQuery` parameter
 */
@ExperimentalEvscApi
suspend fun <T : Table> DatabaseClient<*>.insert(
    table: T,
    selectQuery: AbstractQuery<*>,
    columns: List<Column<*>>? = null,
    createStatementWithExposedTransaction: Boolean = config.autoExposedTransaction || columns == null
) =
    executeUpdate(buildStatement {
        optionalStatementPreparationExposedTransaction(createStatementWithExposedTransaction) {
            table.insert(selectQuery, columns)
        }
    })

/**
 * An alias of the `INSERT SELECT` overload of [insert].
 */
@ExperimentalEvscApi
suspend fun <T : Table> DatabaseClient<*>.insertSelect(
    table: T,
    selectQuery: AbstractQuery<*>,
    columns: List<Column<*>>? = null,
    createStatementWithExposedTransaction: Boolean = config.autoExposedTransaction || columns == null
) =
    insert(table, selectQuery, columns, createStatementWithExposedTransaction)

/**
 * @see StatementBuilder.insertIgnore the overload with `selectQuery` parameter
 */
@ExperimentalEvscApi
suspend fun <T : Table> DatabaseClient<*>.insertIgnore(
    table: T,
    selectQuery: AbstractQuery<*>,
    columns: List<Column<*>>? = null,
    createStatementWithExposedTransaction: Boolean = config.autoExposedTransaction || columns == null
) =
    executeUpdate(buildStatement {
        optionalStatementPreparationExposedTransaction(createStatementWithExposedTransaction) {
            table.insertIgnore(selectQuery, columns)
        }
    })

/**
 * An alias of the `INSERT SELECT` overload of [insertIgnore].
 */
@ExperimentalEvscApi
suspend fun <T : Table> DatabaseClient<*>.insertIgnoreSelect(
    table: T,
    selectQuery: AbstractQuery<*>,
    columns: List<Column<*>>? = null,
    createStatementWithExposedTransaction: Boolean = config.autoExposedTransaction || columns == null
) =
    insertIgnore(table, selectQuery, columns, createStatementWithExposedTransaction)


suspend fun <T : Table> DatabaseClient<*>.update(
    table: T, where: (() -> Op<Boolean>)? = null, limit: Int? = null, body: T.(UpdateStatement) -> Unit
) =
    executeUpdate(buildStatement { table.update(where, limit, body) })


@Deprecated(
    DatabaseClient.SELECT_BATCH_QUERY_WITH_FIELD_SET_DEPRECATED_MESSAGE,
    ReplaceWith("this.batchSelect(data, { columnSet.buildQuery(it) })")
)
@ExperimentalEvscApi
suspend fun <T : ColumnSet, E> DatabaseClient<*>.batchSelect(
    columnSet: T, data: Iterable<E>, buildQuery: T.(E) -> Query
): Sequence<RowSet<ResultRow>> =
    executeBatchQuery(columnSet, data.asSequence().map { columnSet.buildQuery(it) }.asIterable())

/**
 * This function may be very rarely used, as [eq] conditions of multiple statements can usually be combined into an [inList] or [eq] [anyFrom] select query.
 */
@ExperimentalEvscApi
suspend fun <E> DatabaseClient<*>.batchSelect(
    data: Iterable<E>,
    buildQuery: (E) -> Query,
    getFieldExpressionSetWithExposedTransaction: Boolean = config.autoExposedTransaction,
): Sequence<RowSet<ResultRow>> =
    executeBatchQuery(
        data.asSequence().map { buildQuery(it) }.asIterable(),
        getFieldExpressionSetWithExposedTransaction
    )

@Deprecated(
    "Renamed to `batchSelect` and the signature updated.",
    ReplaceWith("this.batchSelect(data, { columnSet.buildQuery(it) })")
)
@ExperimentalEvscApi
suspend fun <E> DatabaseClient<*>.selectBatch(
    fieldSet: FieldSet, buildQuery: FieldSet.(E) -> Query, data: Iterable<E>
): Sequence<RowSet<ResultRow>> =
    batchSelect(fieldSet as ColumnSet, data, buildQuery)

// TODO more params such as `ignoreErrors` and `shouldReturnGeneratedValues`
/**
 * @see DatabaseClient.executeBatchUpdate
 * @see org.jetbrains.exposed.v1.jdbc.batchInsert
 * This function differs from Exposed's [org.jetbrains.exposed.v1.jdbc.batchInsert]
 * in that it doesn't use [StatementBuilder.batchInsert] or [BatchInsertStatement].
 */
@ExperimentalEvscApi
suspend fun <T : Table, E> DatabaseClient<*>.batchInsert(
    table: T, data: Iterable<E>, body: T.(InsertStatement<Number>, E) -> Unit
) =
    executeBatchUpdate(data.asSequence().map { element ->
        buildStatement { table.insert { body(it, element) } }
    }.asIterable())
//.forEach { dbAssert(it == 1) } // The count is not 1 for Oracle.

/**
 * @see DatabaseClient.executeBatchUpdate
 * @see org.jetbrains.exposed.v1.jdbc.batchInsert
 * @see batchInsert
 */
@ExperimentalEvscApi
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
@Deprecated(
    "Use `executeBatchUpdate` directly with `InsertSelectStatement`s.",
    ReplaceWith("this.executeBatchUpdate(statements)")
)
@ExperimentalEvscApi
suspend fun DatabaseClient<*>.batchInsertSelect(statements: Iterable<InsertSelectStatement>) =
    executeBatchUpdate(statements)

/**
 * @see DatabaseClient.executeBatchUpdate
 * @see sortDataAndBatchUpdate
 */
@ExperimentalEvscApi
suspend fun <T : Table, E> DatabaseClient<*>.batchUpdate(
    table: T,
    data: Iterable<E>,
    where: (E) -> Op<Boolean>, // Optional/Nullable `where: ((E) -> Op<Boolean>)? = null` is meaningless here.
    limit: Int? = null,
    body: T.(UpdateStatement, E) -> Unit
) =
    executeBatchUpdate(data.asSequence().map { element ->
        buildStatement { table.update({ where(element) }, limit) { body(it, element) } }
    }.asIterable())

/**
 * @return a sequence indicating whether each update statement is updated in the batch.
 * @see batchUpdate
 */
@ExperimentalEvscApi
suspend fun <T : Table, E> DatabaseClient<*>.batchSingleOrNoUpdate(
    table: T,
    data: Iterable<E>,
    where: (E) -> Op<Boolean>,
    limit: Int? = null,
    body: T.(UpdateStatement, E) -> Unit
): Sequence<Boolean> =
    batchUpdate(table, data, where, limit, body).map { it.singleOrNoUpdate() }

/**
 * @see sortDataAndExecuteBatch
 * @see batchUpdate
 */
@ExperimentalEvscApi
suspend fun <T : Table, E, SelectorResultT : Comparable<SelectorResultT>> DatabaseClient<*>.sortDataAndBatchUpdate(
    table: T,
    data: Iterable<E>, selector: (E) -> SelectorResultT,
    where: (E) -> Op<Boolean>, limit: Int? = null, body: T.(UpdateStatement, E) -> Unit
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
