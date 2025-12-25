package com.huanshankeji.exposedvertxsqlclient

@RequiresOptIn(
    "This API is based on Vert.x's support for Unix domain sockets and is experimental. " +
            "It has not been tested against all supported databases and OSs (only tested against PostgreSQL on Linux by us internally). " +
            "It may be changed in the future without notice.",
    RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalUnixDomainSocketApi