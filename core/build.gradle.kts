plugins {
    `lib-conventions`
}

dependencies {
    api(commonDependencies.exposed.core()) // TODO: use `implementation` when possible
    api(commonDependencies.exposed.module("jdbc"))
    api(commonDependencies.kotlinCommon.exposed())

    with(commonDependencies.vertx) {
        implementation(platformStackDepchain())
        api(moduleWithoutVersion("sql-client")) // TODO: use `implementation` when possible
        implementation(moduleWithoutVersion("lang-kotlin"))
        implementation(moduleWithoutVersion("lang-kotlin-coroutines"))
    }
    implementation(commonDependencies.kotlinCommon.vertx())

    implementation(commonDependencies.kotlinCommon.core())
    implementation(commonDependencies.arrow.core())

    implementation(commonDependencies.kotlinCommon.net())

    api(commonDependencies.kotlinCommon.coroutines()) // Exposed the `use` function
}
