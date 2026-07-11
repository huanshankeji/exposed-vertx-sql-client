pluginManagement {
    repositories {
        gradlePluginPortal()
        exclusiveContent {
            forRepository {
                mavenLocal()
            }
            forRepository {
                maven {
                    // Resolves the gradle-common settings plugin when it is not in mavenLocal().
                    // Mirrors gradle-common credential resolution; its APIs cannot be called from settings.gradle.kts:
                    // https://github.com/huanshankeji/gradle-common/blob/main/kotlin-common/gradle-library/src/main/kotlin/com/huanshankeji/github/packages/maven/GithubPackagesMavenRegistry.kt
                    url = uri("https://maven.pkg.github.com/huanshankeji/gradle-common")
                    credentials {
                        with(providers) {
                            username = gradleProperty("gpr.user").orElse(gradleProperty("gprUser")).getOrNull()
                            password = gradleProperty("gpr.key").orElse(gradleProperty("gprKey")).getOrNull()
                        }
                    }
                }
            }
            filter {
                includeVersionByRegex("com\\.huanshankeji", ".*", ".*-dev-commit-[0-9a-f]+.*")
            }
        }
    }
}

plugins {
    val gradleCommonPluginsVersion = "0.12.0-dev-commit-ac3e42c6941a896568c6eab78cfbb9c9f0ce50bf"
    id("com.huanshankeji.base-settings-conventions") version gradleCommonPluginsVersion
    id("com.huanshankeji.team.gitversioning.public-open-source-dependency-repositories") version gradleCommonPluginsVersion
    id("org.jetbrains.kotlinx.kover.aggregation") version "0.9.4"
}

publicOpenSourceDependencyRepositories {
    mavenCentralExcludingHuanshankeji()
    githubPackages("kotlin-common", "exposed-gadt-mapping")
}

rootProject.name = "exposed-vertx-sql-client"

include("core")
include("crud")
include("crud-with-mapper")
include("postgresql")
include("mysql")
include("oracle")
include("mssql")
include("integrated")

fun ProjectDescriptor.setProjectConcatenatedNames(prefix: String) {
    name = prefix + name
    for (child in children)
        child.setProjectConcatenatedNames("$name-")
}
rootProject.setProjectConcatenatedNames("")

// https://kotlin.github.io/kotlinx-kover/gradle-plugin/aggregated.html
kover {
    enableCoverage()
    reports {
        excludedProjects.add(":exposed-vertx-sql-client-integrated")
        /*
        Not all deprecated APIs are excluded from test coverage,
        for example, deprecated member methods such as the deprecated `DatabaseClient.executeBatchQuery` overload seems to be included,
        which seems to be a limitation of Kover.
         */
        // Excluding "java.lang.Deprecated" too does not help.
        excludesAnnotatedBy.add("kotlin.Deprecated")
    }
}
