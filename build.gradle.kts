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

    // Configure kover to aggregate coverage from all subprojects
    subprojects.forEach { kover(it) }
}

apiValidation {
    ignoredProjects += "exposed-vertx-sql-client-integrated"
}
