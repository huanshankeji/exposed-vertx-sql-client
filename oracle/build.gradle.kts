import com.huanshankeji.cpnProject

plugins {
    `lib-conventions`
}

dependencies {
    with(commonDependencies.vertx) { implementation(platformStackDepchain()) } // needed
    implementation(cpnProject(project, ":core"))

    // TODO remove the Oracle JDBC dependency when there is no need to to generate SQLs with an Exposed transaction
    implementation("com.oracle.database.jdbc:ojdbc11:${DependencyVersions.oracleJdbc}")
    api(commonDependencies.vertx.moduleWithoutVersion("oracle-client")) // `api` used because `OracleConnection` has to be exposed
    //implementation(commonDependencies.kotlinCommon.core()) // for `Untested`
    //implementation(commonDependencies.kotlinCommon.vertx())
}
