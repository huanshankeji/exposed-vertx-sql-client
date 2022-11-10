package com.huanshankeji.exposedvertxsqlclient.classpropertymapping

import com.huanshankeji.exposed.datamapping.classproperty.ClassPropertyColumnMappings
import com.huanshankeji.exposed.datamapping.classproperty.ReflectionBasedClassPropertyDataMapper
import io.vertx.sqlclient.Row
import kotlin.reflect.KClass

/**
 * @see ClassPropertyColumnMappings
 */
typealias ClassPropertyColumnIndexMappings<Data> = Nothing // TODO

fun interface ClassPropertyIndexReadMapper<Data : Any> {
    fun rowToData(row: Row): Data
}

/**
 * @see ReflectionBasedClassPropertyDataMapper
 */
class ReflectionBasedClassPropertyIndexReadMapper<Data : Any>(
    val clazz: KClass<Data>,
    val classPropertyColumnIndexMappings: ClassPropertyColumnIndexMappings<Data>
) : ClassPropertyIndexReadMapper<Data> {
    override fun rowToData(row: Row): Data =
        TODO()

    // I am not sure whether implementing writing to database with Vert.x SQL client prepared query indices and Exposed statements is feasible.
}
