// TODO consider moving the code related to only Exposed or Vert.x to "kotlin-common"

import com.huanshankeji.cpnProject

plugins {
    conventions
    id("com.huanshankeji.benchmark.kotlinx-benchmark-jvm-conventions")
}

dependencies {
    with(commonDependencies.vertx) { implementation(platformStackDepchain()) } // needed

    implementation(cpnProject(project, ":core"))
    implementation(cpnProject(project, ":postgresql"))
    implementation(cpnProject(project, ":crud"))
    implementation("com.huanshankeji:exposed-gadt-mapping:${DependencyVersions.exposedAdtMapping}")
    implementation(cpnProject(project, ":crud-with-mapper"))
}

afterEvaluate {
// for the benchmarks
    dependencies {
        with(commonDependencies.vertx) { "benchmarksImplementation"(platformStackDepchain()) } // needed
        // The benchmarks run and "check" passes but the code doesn't resolve without this dependency TODO remove if not needed one day
        "benchmarksImplementation"(cpnProject(project, ":core"))
        "benchmarksImplementation"(cpnProject(project, ":postgresql"))

        with(commonDependencies.testcontainers) {
            "benchmarksImplementation"(platformBom())
            "benchmarksImplementation"(testcontainersPostgresql)
        }
        // Vert.x actually already includes an `slf4j-simple` dependency
        "benchmarksImplementation"(commonDependencies.slf4j.simple())
    }
}
