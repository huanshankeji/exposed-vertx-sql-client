// TODO consider moving the code related to only Exposed or Vert.x to "kotlin-common"

import com.huanshankeji.cpnProject

plugins {
    conventions
    id("io.kotest") version commonVersions.kotest
    id("com.huanshankeji.benchmark.kotlinx-benchmark-jvm-conventions")
}

dependencies {
    implementation(cpnProject(project, ":core"))
    implementation(cpnProject(project, ":postgresql"))
    implementation(cpnProject(project, ":mysql"))
    implementation(cpnProject(project, ":oracle"))
    implementation(cpnProject(project, ":mssql"))
    implementation(cpnProject(project, ":crud"))
    implementation("com.huanshankeji:exposed-gadt-mapping:${DependencyVersions.exposedGadtMapping}")
    implementation(cpnProject(project, ":crud-with-mapper"))

    implementation(cpnProject(project, ":testcontainers"))
    with(commonDependencies.testcontainers) {
        implementation(platformBom())
        // https://testcontainers.com/modules/postgresql/
        implementation(testcontainersPostgresql)
        // https://testcontainers.com/modules/mysql/
        implementation(moduleWithoutVersion("testcontainers-mysql"))
        // https://testcontainers.com/modules/oracle-free/
        implementation(moduleWithoutVersion("testcontainers-oracle-free"))
        // https://testcontainers.com/modules/mssql/
        implementation(moduleWithoutVersion("testcontainers-mssqlserver"))
    }
    val kotlinCommonTestcontainers = "com.huanshankeji:kotlin-common-testcontainers:${commonVersions.kotlinCommon}"
    implementation(kotlinCommonTestcontainers)
    // Select the helper features; the direct Testcontainers dependencies above supply their compile-time types.
    // see https://github.com/huanshankeji/kotlin-common/blob/fd4d4a690fd73130c3bf0b1fc342156c2d4ac9ed/testcontainers/build.gradle.kts#L9
    listOf("postgresql", "mysql", "oracle", "mssql").forEach { feature ->
        implementation(kotlinCommonTestcontainers) {
            capabilities {
                requireCapability("com.huanshankeji:kotlin-common-testcontainers-$feature")
            }
        }
    }


    implementation(commonDependencies.kotest.module("assertions-core"))
    testImplementation(commonDependencies.kotest.module("framework-engine"))
    testImplementation(commonDependencies.kotest.module("extensions-testcontainers"))
    // to resolve "no tests discovered" errors when running `check`
    testRuntimeOnly(commonDependencies.kotest.module("runner-junit5"))

    testRuntimeOnly(commonDependencies.slf4j.simple())

    implementation(libs.r2dbc.spi)
    implementation(libs.r2dbc.postgresql)
    implementation(libs.r2dbc.pool)
    implementation(libs.exposed.r2dbc)
}

// to resolve "no tests discovered" errors when running `check`
tasks.test {
    useJUnitPlatform()
}

afterEvaluate {
// for the benchmarks
    dependencies {
        // TODO remove `"benchmarksImplementation"`s that are redundant?

        // The benchmarks run and "check" passes but the code doesn't resolve without this dependency TODO remove if not needed one day
        "benchmarksImplementation"(cpnProject(project, ":core"))
        "benchmarksImplementation"(cpnProject(project, ":postgresql"))
        "benchmarksImplementation"(cpnProject(project, ":testcontainers")) {
            capabilities {
                requireCapability("com.huanshankeji:exposed-vertx-sql-client-testcontainers-hikaricp")
            }
        }

        // Vert.x actually already includes an `slf4j-simple` dependency
        "benchmarksRuntimeOnly"(commonDependencies.slf4j.simple())
    }
}
