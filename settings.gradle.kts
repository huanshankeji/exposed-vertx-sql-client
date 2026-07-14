@file:OptIn(com.huanshankeji.GradleCommonExperimentalApi::class)

import com.huanshankeji.artifacts.mavenRepositoryHandlerContext
import com.huanshankeji.team.artifacts.mavenCentralExcludingHuanshankeji
import com.huanshankeji.team.gitversioning.opensourcemavenconvention.githubpackages.huanshankejiGithubPackagesOpenSourceMavenConventionProjectRepositories

pluginManagement {
    // Must apply inside this block: Kotlin DSL runs pluginManagement before top-level statements.
    apply(from = "gradle/classpath-bootstrap.gradle.kts")
    @Suppress("UNCHECKED_CAST")
    (extra["repositories"] as RepositoryHandler.() -> Unit)(repositories)
}

buildscript {
    dependencies {
        classpath("com.huanshankeji.team:settings-gradle-plugins:${settings.extra["gradleCommonPluginsVersion"]}")
    }
}

plugins {
    id("com.huanshankeji.base-settings-conventions") version (extra["gradleCommonPluginsVersion"] as String)
    id("org.jetbrains.kotlinx.kover.aggregation") version "0.9.4"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentralExcludingHuanshankeji()
        mavenRepositoryHandlerContext(providers, ::uri) {
            huanshankejiGithubPackagesOpenSourceMavenConventionProjectRepositories("kotlin-common")
            huanshankejiGithubPackagesOpenSourceMavenConventionProjectRepositories("exposed-gadt-mapping")
        }
    }
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
