package com.huanshankeji.exposedvertxsqlclient.sql.mapping

import com.huanshankeji.exposed.datamapping.DataQueryMapper
import com.huanshankeji.exposed.datamapping.DataUpdateMapper
import com.huanshankeji.exposed.datamapping.updateBuilderSetter
import com.huanshankeji.exposedvertxsqlclient.DatabaseClient
import com.huanshankeji.exposedvertxsqlclient.sql.batchInsert
import com.huanshankeji.exposedvertxsqlclient.sql.insertIgnoreSingle
import com.huanshankeji.exposedvertxsqlclient.sql.insertSingle
import com.huanshankeji.exposedvertxsqlclient.toExposedResultRow
import com.huanshankeji.vertx.sqlclient.datamapping.RowDataQueryMapper
import io.vertx.sqlclient.RowSet
import org.jetbrains.exposed.sql.ColumnSet
import org.jetbrains.exposed.sql.FieldSet
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.UpdateBuilder

suspend fun <Data : Any> DatabaseClient<*>.executeQuery(
    query: Query,
    dataQueryMapper: DataQueryMapper<Data>
): RowSet<Data> =
    executeWithMapping(query) { row -> dataQueryMapper.resultRowToData(row.toExposedResultRow(query)) }

suspend fun <Data : Any> DatabaseClient<*>.executeVertxSqlClientRowQuery(
    query: Query, rowDataQueryMapper: RowDataQueryMapper<Data>
): RowSet<Data> =
    executeWithMapping(query, rowDataQueryMapper::rowToData)

suspend fun <Data : Any> DatabaseClient<*>.select(
    columnSet: ColumnSet, dataQueryMapper: DataQueryMapper<Data>, buildQuery: FieldSet.() -> Query
) =
    executeQuery(columnSet.slice(dataQueryMapper.neededColumns).buildQuery(), dataQueryMapper)

suspend fun <Data : Any> DatabaseClient<*>.insertSingle(
    table: Table, data: Data, dataUpdateMapper: DataUpdateMapper<Data>
) =
    insertSingle(table, dataUpdateMapper.updateBuilderSetter(data))

suspend fun <Data : Any> DatabaseClient<*>.insertIgnoreSingle(
    table: Table, data: Data, dataUpdateMapper: DataUpdateMapper<Data>
) =
    insertIgnoreSingle(table, dataUpdateMapper.updateBuilderSetter(data))

fun <Data : Any, ColumnSetT : ColumnSet> DataUpdateMapper<Data>.batchUpdateBuilderSetter():
        ColumnSetT.(UpdateBuilder<*>, Data) -> Unit = { insertStatement, element ->
    setUpdateBuilder(element, insertStatement)
}

// TODO: consider removing the table param by adding it to `DataUpdateMapper`
suspend fun <Data : Any> DatabaseClient<*>.batchInsert(
    table: Table, data: Iterable<Data>, dataUpdateMapper: DataUpdateMapper<Data>
) =
    batchInsert(table, data, dataUpdateMapper.batchUpdateBuilderSetter())
