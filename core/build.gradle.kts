plugins {
    `lib-conventions`
}

dependencies {
    api(commonDependencies.exposed.core()) // TODO: use `implementation` when possible
    // TODO remove the Exposed JDBC dependency when there is no need to to generate SQLs with an Exposed transaction
    api(commonDependencies.exposed.module("jdbc"))
    api(commonDependencies.kotlinCommon.exposed()) // TODO This is only kept to ease migration and is not used any more. Remove in a future release.

    with(commonDependencies.vertx) {
        implementation(platformStackDepchain())
        api(moduleWithoutVersion("sql-client")) // TODO: use `implementation` when possible
        implementation(moduleWithoutVersion("lang-kotlin"))
        implementation(moduleWithoutVersion("lang-kotlin-coroutines"))
    }
    implementation(commonDependencies.kotlinCommon.vertx())

    implementation(commonDependencies.kotlinCommon.core())
    api(commonDependencies.arrow.core()) // expose `Either`

    implementation(commonDependencies.kotlinCommon.net())

    api(commonDependencies.kotlinCommon.coroutines()) // Exposed the `use` function
}
