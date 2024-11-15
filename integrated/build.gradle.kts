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
        // The benchmarks run and "check" passes but the code doesn't resolve without this dependency TODO remove if not needed one day
        "benchmarksImplementation"(cpnProject(project, ":core"))
        "benchmarksImplementation"(cpnProject(project, ":postgresql"))

        with(commonDependencies.testContainers) {
            "benchmarksImplementation"(platformBom())
            "benchmarksImplementation"(postgreSql)
        }
        "benchmarksImplementation"(commonDependencies.slf4j.simple())
    }
}
