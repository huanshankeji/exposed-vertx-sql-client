package com.huanshankeji.vertx.sqlclient.datamapping.classpropertymapping

import com.huanshankeji.exposed.datamapping.classproperty.ClassPropertyColumnMappings
import com.huanshankeji.exposed.datamapping.classproperty.ReflectionBasedClassPropertyDataMapper
import com.huanshankeji.vertx.sqlclient.datamapping.RowDataQueryMapper
import io.vertx.sqlclient.Row
import kotlin.reflect.KClass

// TODO all definitions are made private because they are not complete yet

/**
 * @see ClassPropertyColumnMappings
 */
// since Kotlin 2.0.0: "'Nothing' property type can't be specified with type alias."
private typealias ClassPropertyColumnIndexMappings<Data> = Unit // TODO

private typealias VertxSqlClientRowDataQueryMapper<Data> = RowDataQueryMapper<Data>

/**
 * @see ReflectionBasedClassPropertyDataMapper
 */
private class ReflectionBasedClassPropertyIndexVertxSqlClientRowDataQueryMapper<Data : Any>(
    val clazz: KClass<Data>,
    val classPropertyColumnIndexMappings: ClassPropertyColumnIndexMappings<Data>
) : RowDataQueryMapper<Data> {
    override fun rowToData(row: Row): Data =
        TODO()

    // I am not sure whether implementing writing to database with Vert.x SQL client prepared query indices and Exposed statements is feasible.
}
