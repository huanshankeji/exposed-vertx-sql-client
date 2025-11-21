import com.huanshankeji.cpnProject

plugins {
    `lib-conventions`
}

dependencies {
    with(commonDependencies.vertx) { implementation(platformStackDepchain()) } // needed
    implementation(cpnProject(project, ":core"))

    // TODO remove the MySQL JDBC dependency when there is no need to to generate SQLs with an Exposed transaction
    implementation("com.mysql:mysql-connector-j:${DependencyVersions.mysqlConnectorJ}")
    api(commonDependencies.vertx.moduleWithoutVersion("mysql-client")) // `api` used because `MySQLConnection` has to be exposed
    //implementation(commonDependencies.kotlinCommon.core()) // for `Untested`
    //implementation(commonDependencies.kotlinCommon.vertx())
}
