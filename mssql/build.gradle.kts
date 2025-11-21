import com.huanshankeji.cpnProject

plugins {
    `lib-conventions`
}

dependencies {
    with(commonDependencies.vertx) { implementation(platformStackDepchain()) } // needed
    implementation(cpnProject(project, ":core"))

    // TODO remove the MSSQL JDBC dependency when there is no need to to generate SQLs with an Exposed transaction
    implementation("com.microsoft.sqlserver:mssql-jdbc:${DependencyVersions.mssqlJdbc}")
    api(commonDependencies.vertx.moduleWithoutVersion("mssql-client")) // `api` used because `MSSQLConnection` has to be exposed
    //implementation(commonDependencies.kotlinCommon.core()) // for `Untested`
    //implementation(commonDependencies.kotlinCommon.vertx())
}
