import com.huanshankeji.cpnProject

plugins {
    `lib-conventions`
}

dependencies {
    with(commonDependencies.vertx) { implementation(platformStackDepchain()) }
    implementation(cpnProject(project, ":core"))
    implementation(cpnProject(project, ":postgresql"))
    implementation(cpnProject(project, ":mysql"))
    implementation(cpnProject(project, ":oracle"))
    implementation(cpnProject(project, ":mssql"))

    api("com.huanshankeji:kotlin-common-testcontainers:${commonVersions.kotlinCommon}")

    with(commonDependencies.testcontainers) {
        api(platformBom())
        api(testcontainersPostgresql)
        api(moduleWithoutVersion("testcontainers-mysql"))
        api(moduleWithoutVersion("testcontainers-oracle-free"))
        api(moduleWithoutVersion("testcontainers-mssqlserver"))
    }

    implementation("com.zaxxer:HikariCP:${DependencyVersions.hikaricp}")
}
