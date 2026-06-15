pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

plugins {
    id("public-open-source-dependency-repositories") version
        "0.12.0-dev-commit-e2dcb9d3d327edd99d80a6dfa041fc47c2db7bbb-dirty-SNAPSHOT"
    id("org.jetbrains.kotlinx.kover.aggregation") version "0.9.4"
}

publicOpenSourceDependencyRepositories {
    huanshankejiMavenLocal()
    githubPackages("exposed-vertx-sql-client", "kotlin-common", "exposed-gadt-mapping", "gradle-common")
    mavenCentralExcludingHuanshankejiNonStable()
}

rootProject.name = "exposed-vertx-sql-client"

include("core")
include("crud")
include("crud-with-mapper")
include("postgresql")
include("mysql")
include("oracle")
include("mssql")
include("integrated")

fun ProjectDescriptor.setProjectConcatenatedNames(prefix: String) {
    name = prefix + name
    for (child in children)
        child.setProjectConcatenatedNames("$name-")
}
rootProject.setProjectConcatenatedNames("")

// https://kotlin.github.io/kotlinx-kover/gradle-plugin/aggregated.html
kover {
    enableCoverage()
    reports {
        excludedProjects.add(":exposed-vertx-sql-client-integrated")
        /*
        Not all deprecated APIs are excluded from test coverage,
        for example, deprecated member methods such as the deprecated `DatabaseClient.executeBatchQuery` overload seems to be included,
        which seems to be a limitation of Kover.
         */
        // Excluding "java.lang.Deprecated" too does not help.
        excludesAnnotatedBy.add("kotlin.Deprecated")
    }
}
