import com.huanshankeji.cpnProject

plugins {
    `lib-conventions`
}

// `MSSQLServerContainer` is only needed for its `connectionConfig()` overload.
java {
    registerFeature("postgresql") {
        usingSourceSet(sourceSets["main"])
    }
    registerFeature("mssql") {
        usingSourceSet(sourceSets["main"])
    }

    registerFeature("hikaricp") {
        usingSourceSet(sourceSets["main"])
    }
}

dependencies {
    api(cpnProject(project, ":core"))
    // Hikari needs the JDBC driver; these helpers do not use the PostgreSQL EVSC client.
    "postgresqlRuntimeOnly"(commonDependencies.postgreSql())

    with(commonDependencies.testcontainers) {
        api(platformBom())
        api(moduleWithoutVersion("testcontainers-jdbc"))
        "mssqlApi"(moduleWithoutVersion("testcontainers-mssqlserver"))
    }

    "hikaricpApi"("com.zaxxer:HikariCP:${DependencyVersions.hikaricp}")
}
