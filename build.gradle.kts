tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}

plugins {
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.18.1"
}

dependencies {
    for (project in subprojects.filterNot { it.name == "exposed-vertx-sql-client-integrated" })
        dokka(project)
}

apiValidation {
    ignoredProjects += "exposed-vertx-sql-client-integrated"
}
