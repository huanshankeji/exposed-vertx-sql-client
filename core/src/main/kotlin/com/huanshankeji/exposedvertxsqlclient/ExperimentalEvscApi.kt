package com.huanshankeji.exposedvertxsqlclient

import kotlin.annotation.AnnotationTarget.*

/**
 * Marks APIs that are experimental in the Exposed Vert.x SQL Client library.
 *
 * These APIs may be changed or removed in future versions without notice.
 * Use with caution in production code.
 */
@RequiresOptIn("This API is experimental. It may be changed in the future without notice.", RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
// The ones commented out are what I think may be used in very few use cases.
@Target(
    CLASS,
    //ANNOTATION_CLASS,
    PROPERTY,
    FIELD,
    VALUE_PARAMETER,
    CONSTRUCTOR,
    FUNCTION,
    PROPERTY_GETTER,
    PROPERTY_SETTER,
    TYPEALIAS
)
annotation class ExperimentalEvscApi