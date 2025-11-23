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
}

// Add kover dependencies to merge coverage from all subprojects
dependencies {
    subprojects.forEach { kover(it) }
}

apiValidation {
    ignoredProjects += "exposed-vertx-sql-client-integrated"
}

// Apply kover to all subprojects
subprojects {
    apply(plugin = "org.jetbrains.kotlinx.kover")
}

// Configure kover aggregation at root level
kover {
    reports {
        total {
            xml {
                onCheck = true
            }
            html {
                onCheck = true
            }
        }
    }
}
