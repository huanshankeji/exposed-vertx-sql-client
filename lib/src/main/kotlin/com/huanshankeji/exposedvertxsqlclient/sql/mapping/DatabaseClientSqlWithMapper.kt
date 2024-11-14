@file:OptIn(InternalApi::class)

package com.huanshankeji.exposedvertxsqlclient.sql.mapping

import com.huanshankeji.InternalApi
import com.huanshankeji.exposed.BuildWhere
import com.huanshankeji.exposed.SELECT_DSL_DEPRECATION_MESSAGE
import com.huanshankeji.exposed.datamapping.DataQueryMapper
import com.huanshankeji.exposed.datamapping.DataUpdateMapper
import com.huanshankeji.exposed.datamapping.updateBuilderSetter
import com.huanshankeji.exposed.deleteIgnoreWhereStatement
import com.huanshankeji.exposed.deleteWhereStatement
import com.huanshankeji.exposedvertxsqlclient.DatabaseClient
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.sql.*
import com.huanshankeji.vertx.sqlclient.datamapping.RowDataQueryMapper
import io.vertx.sqlclient.RowSet
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder

// TODO move to a separate module
// TODO Note that using these DSLs reduces the composability of statements, for example, when moving a query into a subquery. (this statement can be moved into docs some day)

@ExperimentalEvscApi
suspend fun <Data : Any> DatabaseClient<*>.executeQuery(
    query: Query,
    dataQueryMapper: DataQueryMapper<Data>
): RowSet<Data> =
    executeWithMapping(query) { row -> dataQueryMapper.resultRowToData(row.toExposedResultRowWithTransaction(query)) }

@ExperimentalEvscApi
suspend fun <Data : Any> DatabaseClient<*>.executeVertxSqlClientRowQuery(
    query: Query, rowDataQueryMapper: RowDataQueryMapper<Data>
): RowSet<Data> =
    executeWithMapping(query, rowDataQueryMapper::rowToData)

@Deprecated(
    SELECT_DSL_DEPRECATION_MESSAGE,
    ReplaceWith("this.selectWithMapper<Data>(columnSet, dataQueryMapper, buildQuery)")
)
@ExperimentalEvscApi
suspend fun <Data : Any> DatabaseClient<*>.select(
    columnSet: ColumnSet, dataQueryMapper: DataQueryMapper<Data>, buildQuery: FieldSet.() -> Query
) =
    executeQuery(columnSet.slice(dataQueryMapper.neededColumns).buildQuery(), dataQueryMapper)

@ExperimentalEvscApi
suspend fun <Data : Any> DatabaseClient<*>.selectWithMapper(
    columnSet: ColumnSet, dataQueryMapper: DataQueryMapper<Data>, buildQuery: Query.() -> Query
) =
    executeQuery(columnSet.select(dataQueryMapper.neededColumns).buildQuery(), dataQueryMapper)

@ExperimentalEvscApi
suspend fun <Data : Any> DatabaseClient<*>.insert(
    table: Table, data: Data, dataUpdateMapper: DataUpdateMapper<Data>
) =
    insert(table, dataUpdateMapper.updateBuilderSetter(data))

@ExperimentalEvscApi
suspend fun <Data : Any> DatabaseClient<*>.insertIgnore(
    table: Table, data: Data, dataUpdateMapper: DataUpdateMapper<Data>
) =
    insertIgnore(table, dataUpdateMapper.updateBuilderSetter(data))

fun <Data : Any, ColumnSetT : ColumnSet> DataUpdateMapper<Data>.batchUpdateBuilderSetter():
        ColumnSetT.(UpdateBuilder<*>, Data) -> Unit = { insertStatement, element ->
    setUpdateBuilder(element, insertStatement)
}

// TODO: consider removing the table param by adding it to `DataUpdateMapper`

@ExperimentalEvscApi
suspend fun <Data : Any> DatabaseClient<*>.batchInsert(
    table: Table, data: Iterable<Data>, dataUpdateMapper: DataUpdateMapper<Data>
) =
    batchInsert(table, data, dataUpdateMapper.batchUpdateBuilderSetter())

@ExperimentalEvscApi
suspend fun <Data : Any> DatabaseClient<*>.batchInsertIgnore(
    table: Table, data: Iterable<Data>, dataUpdateMapper: DataUpdateMapper<Data>
) =
    batchInsertIgnore(table, data, dataUpdateMapper.batchUpdateBuilderSetter())


/**
 * In most cases you should specify the fields to update in a more detailed way instead of using this function.
 */
@ExperimentalEvscApi
suspend fun <Data : Any> DatabaseClient<*>.update(
    table: Table, where: BuildWhere? = null, limit: Int? = null, data: Data, dataUpdateMapper: DataUpdateMapper<Data>
) =
    update(table, where, limit, dataUpdateMapper.updateBuilderSetter(data))

suspend fun <T : Table> DatabaseClient<*>.deleteWhere(
    table: T, limit: Int? = null, offset: Long? = null, op: T.(ISqlExpressionBuilder) -> Op<Boolean>
) =
    executeUpdate(table.deleteWhereStatement(limit, offset, op))

suspend fun <T : Table> DatabaseClient<*>.deleteIgnoreWhere(
    table: T, limit: Int? = null, offset: Long? = null, op: T.(ISqlExpressionBuilder) -> Op<Boolean>
) =
    executeUpdate(table.deleteIgnoreWhereStatement(limit, offset, op))
