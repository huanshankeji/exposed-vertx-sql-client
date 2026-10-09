import com.huanshankeji.cpnProject

plugins {
    `lib-conventions`
}

// `MSSQLServerContainer` is only needed for its `connectionConfig()` overload.
java {
    registerFeature("mssql") {
        usingSourceSet(sourceSets["main"])
    }
}

dependencies {
    with(commonDependencies.vertx) { implementation(platformStackDepchain()) }
    api(cpnProject(project, ":core"))
    // Hikari needs the JDBC driver; these helpers do not use the PostgreSQL EVSC client.
    runtimeOnly(commonDependencies.postgreSql())

    api("com.zaxxer:HikariCP:${DependencyVersions.hikaricp}")

    with(commonDependencies.testcontainers) {
        api(platformBom())
        api(testcontainers)
        "mssqlApi"(moduleWithoutVersion("testcontainers-mssqlserver"))
    }
}
