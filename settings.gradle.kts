rootProject.name = "exposed-vertx-sql-client"

include("core")
include("crud")
include("crud-with-mapper")
include("postgresql")
include("mysql")
include("db2")
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
