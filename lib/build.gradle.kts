plugins {
    conventions
    with(commonGradleClasspathDependencies.kotlinx.benchmark) { applyPluginWithVersion() }
    kotlin("plugin.allopen") version commonVersions.kotlin
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


private val BENCHMAKRS = "benchmarks"

sourceSets.create(BENCHMAKRS)

dependencies {
    "benchmarksImplementation"(sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath)
    "benchmarksImplementation"(commonDependencies.kotlinx.benchmark.runtime())
    with(commonDependencies.testContainers) {
        "benchmarksImplementation"(platformBom())
        "benchmarksImplementation"(postgreSql)
    }
    "benchmarksImplementation"(commonDependencies.slf4j.simple())
}

benchmark {
    targets {
        register(BENCHMAKRS)
    }
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}
