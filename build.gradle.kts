tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}

plugins {
    id("org.jetbrains.dokka")
}

dependencies {
    dokka(project(":exposed-vertx-sql-client-postgresql"))
}
