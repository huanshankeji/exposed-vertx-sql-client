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
        // NOTE: The `excludesAnnotatedBy` filter is intended to exclude classes and functions annotated with
        // `@kotlin.Deprecated` from coverage reports. However, as of Kover 0.9.3/0.9.4, this filter appears to
        // not work for method-level annotations in the Kover Aggregated Plugin (the prototype settings plugin).
        // This is a known limitation documented here as a workaround is not readily available.
        // See: https://kotlin.github.io/kotlinx-kover/gradle-plugin/aggregated.html
        // The Kover team plans to migrate coverage functionality into the Kotlin Gradle Plugin itself (issue #724).
        excludesAnnotatedBy.add("kotlin.Deprecated")
    }
}
