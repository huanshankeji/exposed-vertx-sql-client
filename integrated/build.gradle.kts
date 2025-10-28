// TODO consider moving the code related to only Exposed or Vert.x to "kotlin-common"

import com.huanshankeji.cpnProject
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    conventions
    id("com.huanshankeji.benchmark.kotlinx-benchmark-jvm-conventions")
}

tasks.named<KotlinCompilationTask<*>>("compileTestKotlin").configure {
    compilerOptions.freeCompilerArgs.add("-Xnested-type-aliases")
}

dependencies {
    with(commonDependencies.vertx) { implementation(platformStackDepchain()) } // needed

    implementation(cpnProject(project, ":core"))
    implementation(cpnProject(project, ":postgresql"))
    implementation(cpnProject(project, ":mysql"))
    implementation(cpnProject(project, ":crud"))
    implementation("com.huanshankeji:exposed-gadt-mapping:${DependencyVersions.exposedGadtMapping}")
    implementation(cpnProject(project, ":crud-with-mapper"))

    // Test dependencies
    with(commonDependencies.vertx) { testImplementation(platformStackDepchain()) }
    testImplementation(cpnProject(project, ":core"))
    testImplementation(cpnProject(project, ":postgresql"))
    testImplementation(cpnProject(project, ":mysql"))
    testImplementation(cpnProject(project, ":crud"))
    testImplementation(cpnProject(project, ":crud-with-mapper"))
    testImplementation("com.huanshankeji:exposed-gadt-mapping:${DependencyVersions.exposedGadtMapping}")
    
    with(commonDependencies.testcontainers) {
        testImplementation(platformBom())
        testImplementation(testcontainersPostgresql)
        testImplementation("org.testcontainers:mysql:1.20.4")
    }
    
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation(commonDependencies.slf4j.simple())
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
