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
    implementation(cpnProject(project, ":oracle"))
    implementation(cpnProject(project, ":mssql"))
    implementation(cpnProject(project, ":crud"))
    implementation("com.huanshankeji:exposed-gadt-mapping:${DependencyVersions.exposedGadtMapping}")
    implementation(cpnProject(project, ":crud-with-mapper"))

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


    implementation(commonDependencies.kotest.module("assertions-core"))
    testImplementation(commonDependencies.kotest.module("framework-engine"))
    testImplementation(commonDependencies.kotest.module("extensions-testcontainers"))
    // to resolve "no tests discovered" errors when running `check`
    testRuntimeOnly(commonDependencies.kotest.module("runner-junit5"))

    testRuntimeOnly(commonDependencies.slf4j.simple())

    implementation("com.zaxxer:HikariCP:${DependencyVersions.hikaricp}")

    implementation(libs.r2dbc.spi)
    implementation(libs.r2dbc.postgresql)
    implementation(libs.r2dbc.pool)
    implementation(libs.exposed.r2dbc)
}

// to resolve "no tests discovered" errors when running `check`
tasks.test {
    useJUnitPlatform()
}

// Profiling tasks for TfbBatchUpdateBenchmark
// These allow running the benchmark code with async-profiler for generating flame graphs
tasks.register<JavaExec>("profileWithDatabaseProvider") {
    group = "profiling"
    description = "Profile TfbBatchUpdate with DatabaseExposedTransactionProvider"
    mainClass.set("com.huanshankeji.exposedvertxsqlclient.integrated.profiling.TfbBatchUpdateProfilingMainKt")
    classpath = sourceSets.main.get().runtimeClasspath
    args = listOf("database", "10")
    
    // Set JVM args for async-profiler if the path is provided
    // Using itimer for CPU profiling when perf_events is not available
    val asyncProfilerPath = project.findProperty("asyncProfilerPath") as String?
    val outputFile = project.findProperty("profileOutputFile") as String? ?: "${project.rootDir}/profiling-results/database_provider_profile.html"
    if (asyncProfilerPath != null) {
        jvmArgs = listOf("-agentpath:$asyncProfilerPath=start,event=itimer,file=$outputFile")
    }
}

tasks.register<JavaExec>("profileWithJdbcProvider") {
    group = "profiling"
    description = "Profile TfbBatchUpdate with JdbcTransactionExposedTransactionProvider"
    mainClass.set("com.huanshankeji.exposedvertxsqlclient.integrated.profiling.TfbBatchUpdateProfilingMainKt")
    classpath = sourceSets.main.get().runtimeClasspath
    args = listOf("jdbc", "10")
    
    // Set JVM args for async-profiler if the path is provided
    // Using itimer for CPU profiling when perf_events is not available
    val asyncProfilerPath = project.findProperty("asyncProfilerPath") as String?
    val outputFile = project.findProperty("profileOutputFile") as String? ?: "${project.rootDir}/profiling-results/jdbc_provider_profile.html"
    if (asyncProfilerPath != null) {
        jvmArgs = listOf("-agentpath:$asyncProfilerPath=start,event=itimer,file=$outputFile")
    }
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
