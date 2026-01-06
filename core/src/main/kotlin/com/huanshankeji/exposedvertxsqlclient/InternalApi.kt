package com.huanshankeji.exposedvertxsqlclient

/**
 * Marks APIs that are internal to the Exposed Vert.x SQL Client library.
 *
 * These APIs are not intended for public use and may be changed or removed without notice.
 * They should not be used outside of this library.
 */
@RequiresOptIn("This API is internal in the Exposed Vert.x SQL Client library. It may be changed in the future without notice.")
@Retention(AnnotationRetention.BINARY)
annotation class InternalApi