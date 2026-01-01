tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}

plugins {
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.18.1"
    id("org.jetbrains.kotlinx.kover")
}

dependencies {
    dokka(project(":exposed-vertx-sql-client-postgresql"))

    // Kover dependencies for merged coverage report
    kover(project(":exposed-vertx-sql-client-core"))
    kover(project(":exposed-vertx-sql-client-crud"))
    kover(project(":exposed-vertx-sql-client-crud-with-mapper"))
    kover(project(":exposed-vertx-sql-client-postgresql"))
    kover(project(":exposed-vertx-sql-client-mysql"))
    kover(project(":exposed-vertx-sql-client-oracle"))
    kover(project(":exposed-vertx-sql-client-mssql"))
    // Tests are in the integrated module
    kover(project(":exposed-vertx-sql-client-integrated"))
}

apiValidation {
    ignoredProjects += "exposed-vertx-sql-client-integrated"
}

// Kover configuration to exclude deprecated APIs and the integrated module from coverage reports
kover {
    reports {
        total {
            filters {
                excludes {
                    annotatedBy("kotlin.Deprecated")
                    // Exclude the integrated module (examples and tests) from coverage calculations
                    projects.add(":exposed-vertx-sql-client-integrated")
                }
            }
        }
    }
}
