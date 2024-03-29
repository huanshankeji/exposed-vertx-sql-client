plugins {
    conventions
    id("com.huanshankeji.benchmark.kotlinx-benchmark-jvm-conventions")
}

dependencies {
    api(commonDependencies.exposed.core()) // TODO: use `implementation` when possible
    // TODO: remove the Exposed JDBC dependency and the PostgresSQL dependency when there is no need to to generate SQLs with an Exposed transaction
    runtimeOnly(commonDependencies.exposed.module("jdbc"))
    api(commonDependencies.kotlinCommon.exposed())
    implementation("com.huanshankeji:exposed-adt-mapping:${DependencyVersions.exposedAdtMapping}")

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
}


// for PostgreSQL
dependencies {
    runtimeOnly(commonDependencies.postgreSql())
    implementation(commonDependencies.vertx.moduleWithoutVersion("pg-client"))
}

afterEvaluate {
// for the benchmarks
    dependencies {
        with(commonDependencies.testContainers) {
            "benchmarksImplementation"(platformBom())
            "benchmarksImplementation"(postgreSql)
        }
        "benchmarksImplementation"(commonDependencies.slf4j.simple())
    }
}