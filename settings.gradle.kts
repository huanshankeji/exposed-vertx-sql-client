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

kover {
    enableCoverage()
    reports {
        excludedProjects.add(":exposed-vertx-sql-client-integrated")
        excludesAnnotatedBy.add("kotlin.Deprecated") // TODO verify if this works as intended
    }
}
