package com.huanshankeji.exposedvertxsqlclient.sql.mapping

import com.huanshankeji.exposed.datamapping.DataQueryMapper
import com.huanshankeji.exposed.datamapping.DataUpdateMapper
import com.huanshankeji.exposed.datamapping.updateBuilderSetter
import com.huanshankeji.exposedvertxsqlclient.DatabaseClient
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.sql.batchInsert
import com.huanshankeji.exposedvertxsqlclient.sql.batchInsertIgnore
import com.huanshankeji.exposedvertxsqlclient.sql.insert
import com.huanshankeji.exposedvertxsqlclient.sql.insertIgnore
import com.huanshankeji.exposedvertxsqlclient.toExposedResultRow
import com.huanshankeji.vertx.sqlclient.datamapping.RowDataQueryMapper
import io.vertx.sqlclient.RowSet
import org.jetbrains.exposed.sql.ColumnSet
import org.jetbrains.exposed.sql.FieldSet
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.UpdateBuilder

@ExperimentalEvscApi
suspend fun <Data : Any> DatabaseClient<*>.executeQuery(
    query: Query,
    dataQueryMapper: DataQueryMapper<Data>
): RowSet<Data> =
    executeWithMapping(query) { row -> dataQueryMapper.resultRowToData(row.toExposedResultRow(query)) }

@ExperimentalEvscApi
suspend fun <Data : Any> DatabaseClient<*>.executeVertxSqlClientRowQuery(
    query: Query, rowDataQueryMapper: RowDataQueryMapper<Data>
): RowSet<Data> =
    executeWithMapping(query, rowDataQueryMapper::rowToData)

@ExperimentalEvscApi
suspend fun <Data : Any> DatabaseClient<*>.select(
    columnSet: ColumnSet, dataQueryMapper: DataQueryMapper<Data>, buildQuery: FieldSet.() -> Query
) =
    executeQuery(columnSet.slice(dataQueryMapper.neededColumns).buildQuery(), dataQueryMapper)

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
