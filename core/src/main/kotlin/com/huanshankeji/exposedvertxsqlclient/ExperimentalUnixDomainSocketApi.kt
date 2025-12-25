package com.huanshankeji.exposedvertxsqlclient

@RequiresOptIn(
    "This API is based on Vert.x's support for Unix domain sockets and is experimental. " +
            "It has not been thoroughly tested across all supported databases and OSs; so far, it has only been verified by us internally with PostgreSQL on Linux. " +
            "It may be changed in the future without notice.",
    RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalUnixDomainSocketApi