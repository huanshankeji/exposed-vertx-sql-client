// TODO consider moving the code related to only Exposed or Vert.x to "kotlin-common"

import com.huanshankeji.cpnProject

plugins {
    conventions
    id("io.kotest") version commonVersions.kotest
    id("com.huanshankeji.benchmark.kotlinx-benchmark-jvm-conventions")
}

dependencies {
    with(commonDependencies.vertx) { implementation(platformStackDepchain()) } // needed

    implementation(cpnProject(project, ":core"))
    implementation(cpnProject(project, ":postgresql"))
    implementation(cpnProject(project, ":mysql"))
    implementation(cpnProject(project, ":crud"))
    implementation("com.huanshankeji:exposed-gadt-mapping:${DependencyVersions.exposedGadtMapping}")
    implementation(cpnProject(project, ":crud-with-mapper"))

    with(commonDependencies.testcontainers) {
        implementation(platformBom())
        implementation(testcontainersPostgresql)
        implementation(moduleWithoutVersion("testcontainers-mysql"))
    }


    testImplementation(commonDependencies.kotest.module("framework-engine"))
    testImplementation(commonDependencies.kotest.module("extensions-testcontainers"))
    // to resolve "no tests discovered" errors when running `check`
    testRuntimeOnly(commonDependencies.kotest.module("runner-junit5"))

    testRuntimeOnly(commonDependencies.slf4j.simple())
}

// to resolve "no tests discovered" errors when running `check`
tasks.test {
    useJUnitPlatform()
}

afterEvaluate {
// for the benchmarks
    dependencies {
        // TODO remove `"benchmarksImplementation"`s that are redundant?

        with(commonDependencies.vertx) { "benchmarksImplementation"(platformStackDepchain()) } // needed
        // The benchmarks run and "check" passes but the code doesn't resolve without this dependency TODO remove if not needed one day
        "benchmarksImplementation"(cpnProject(project, ":core"))
        "benchmarksImplementation"(cpnProject(project, ":postgresql"))

        // Vert.x actually already includes an `slf4j-simple` dependency
        "benchmarksRuntimeOnly"(commonDependencies.slf4j.simple())
    }
}
