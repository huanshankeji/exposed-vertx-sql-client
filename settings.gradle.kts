rootProject.name = "exposed-vertx-sql-client"
include("lib")
project(":lib").name = rootProject.name + "-postgresql"

// for Dokka
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
