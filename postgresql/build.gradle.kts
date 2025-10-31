import com.huanshankeji.cpnProject

plugins {
    `lib-conventions`
}

dependencies {
    with(commonDependencies.vertx) { implementation(platformStackDepchain()) } // needed
    implementation(cpnProject(project, ":core"))

    // TODO remove the Exposed JDBC dependency and the PostgresSQL dependency when there is no need to to generate SQLs with an Exposed transaction
    runtimeOnly(commonDependencies.postgreSql())
    // For Unix domain socket support in JDBC
    runtimeOnly("com.kohlschutter.junixsocket:junixsocket-core:${DependencyVersions.junixsocket}")
    api(commonDependencies.vertx.moduleWithoutVersion("pg-client")) // `api` used because `PgConnection` has to be exposed
    implementation(commonDependencies.kotlinCommon.core()) // for `Untested`
    //implementation(commonDependencies.kotlinCommon.vertx()) // for `PgPoolOptions.setUpConventionally`
}
