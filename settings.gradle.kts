plugins {
    id("org.jetbrains.kotlinx.kover.aggregation") version "0.9.3"
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

// for Dokka
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

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
        excludesAnnotatedBy.add("kotlin.Deprecated")
    }
}
