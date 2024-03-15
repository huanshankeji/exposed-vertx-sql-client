rootProject.name = "exposed-vertx-sql-client"
include("lib")
project(":lib").name = rootProject.name + "-postgresql"
