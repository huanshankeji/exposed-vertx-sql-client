rootProject.name = "exposed-vertx-sql-client"

include("core")
include("sql-dsl")
include("sql-dsl-with-mapper")
include("mysql")
include("postgresql")
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
