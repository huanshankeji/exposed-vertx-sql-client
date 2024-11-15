import com.huanshankeji.cpnProject

plugins {
    conventions
    id("com.huanshankeji.benchmark.kotlinx-benchmark-jvm-conventions")
}

dependencies {
    with(commonDependencies.vertx) { implementation(platformStackDepchain()) } // needed

    implementation(cpnProject(project, ":core"))

    testImplementation("com.huanshankeji:exposed-adt-mapping:${DependencyVersions.exposedAdtMapping}")
    testImplementation(cpnProject(project, ":sql-dsl-with-mapper"))
}

afterEvaluate {
// for the benchmarks
    dependencies {
        "benchmarksImplementation"(cpnProject(project, ":postgresql"))

        with(commonDependencies.testContainers) {
            "benchmarksImplementation"(platformBom())
            "benchmarksImplementation"(postgreSql)
        }
        "benchmarksImplementation"(commonDependencies.slf4j.simple())
    }
}
