package com.huanshankeji.exposedvertxsqlclient.classpropertymapping

import com.huanshankeji.exposed.classpropertymapping.ClassPropertyColumnMappings
import com.huanshankeji.exposed.classpropertymapping.ReflectionBasedClassPropertyMapper
import io.vertx.sqlclient.Row
import kotlin.reflect.KClass

/**
 * @see ClassPropertyColumnMappings
 */
typealias ClassPropertyColumnIndexMappings<Data> = Nothing // TODO

/**
 * @see ReflectionBasedClassPropertyMapper
 */
class ReflectionBasedClassPropertyIndexMapper<Data : Any>(
    val clazz: KClass<Data>,
    val classPropertyColumnIndexMappings: ClassPropertyColumnIndexMappings<Data>
) {
    fun rowToData(row: Row): Data =
        TODO()

    // I am not sure whether implementing writing to database with Vert.x SQL client prepared query indices and Exposed statements is feasible.
}
