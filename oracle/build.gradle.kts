import com.huanshankeji.cpnProject

plugins {
    `lib-conventions`
}

dependencies {
    implementation(cpnProject(project, ":core"))

    // TODO remove the Oracle JDBC dependency when there is no need to to generate SQLs with an Exposed transaction
    implementation("com.oracle.database.jdbc:ojdbc11:${DependencyVersions.oracleJdbc}")
    with(commonDependencies.vertx) {
        // Export the BOM so consumers can resolve the versionless public Vert.x client dependency.
        // `implementation(:core)` supplies its BOM locally, but hides it from consumers' compile classpaths.
        api(platformStackDepchain())
        api(moduleWithoutVersion("oracle-client")) // `api` used because `OracleConnection` has to be exposed
    }
    //implementation(commonDependencies.kotlinCommon.core()) // for `Untested`
    //implementation(commonDependencies.kotlinCommon.vertx())
}
