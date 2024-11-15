package com.huanshankeji.exposedvertxsqlclient.classpropertymapping

import com.huanshankeji.exposed.datamapping.classproperty.ClassPropertyColumnMappings
import com.huanshankeji.exposed.datamapping.classproperty.ReflectionBasedClassPropertyDataMapper
import com.huanshankeji.vertx.sqlclient.datamapping.RowDataQueryMapper
import io.vertx.sqlclient.Row
import kotlin.reflect.KClass

/**
 * @see ClassPropertyColumnMappings
 */
// since Kotlin 2.0.0: "'Nothing' property type can't be specified with type alias."
typealias ClassPropertyColumnIndexMappings<Data> = Unit // TODO

typealias VertxSqlClientRowDataQueryMapper<Data> = RowDataQueryMapper<Data>

/**
 * @see ReflectionBasedClassPropertyDataMapper
 */
class ReflectionBasedClassPropertyIndexVertxSqlClientRowDataQueryMapper<Data : Any>(
    val clazz: KClass<Data>,
    val classPropertyColumnIndexMappings: ClassPropertyColumnIndexMappings<Data>
) : RowDataQueryMapper<Data> {
    override fun rowToData(row: Row): Data =
        TODO()

    // I am not sure whether implementing writing to database with Vert.x SQL client prepared query indices and Exposed statements is feasible.
}
