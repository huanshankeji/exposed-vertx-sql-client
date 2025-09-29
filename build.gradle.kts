tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}

plugins {
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.18.1"
}

dependencies {
    dokka(project(":exposed-vertx-sql-client-postgresql"))
}

apiValidation {
    ignoredProjects += "exposed-vertx-sql-client-integrated"
}
