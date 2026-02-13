package com.huanshankeji.exposedvertxsqlclient

/**
 * Marks APIs that are internal to the Exposed Vert.x SQL Client library.
 *
 * These APIs are not intended for public use and may be changed or removed without notice.
 * They should not be used outside of this library,
 * unless you are experimenting with or trying to create new APIs that extend the functionalities of this library,
 * which could also be contributed back to this library.
 */
@RequiresOptIn("This API is internal in the Exposed Vert.x SQL Client library. It may be changed in the future without notice.")
@Retention(AnnotationRetention.BINARY)
annotation class EvscInternalApi

@Deprecated("Use `EvscInternalApi` instead.", ReplaceWith("EvscInternalApi"))
@RequiresOptIn("This API is internal in the Exposed Vert.x SQL Client library. It may be changed in the future without notice.")
@Retention(AnnotationRetention.BINARY)
annotation class InternalApi
