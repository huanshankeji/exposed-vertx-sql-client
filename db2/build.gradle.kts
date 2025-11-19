import com.huanshankeji.cpnProject

plugins {
    `lib-conventions`
}

dependencies {
    with(commonDependencies.vertx) { implementation(platformStackDepchain()) } // needed
    implementation(cpnProject(project, ":core"))

    // TODO remove the Exposed JDBC dependency and the DB2 dependency when there is no need to to generate SQLs with an Exposed transaction
    runtimeOnly(commonDependencies.exposed.module("jdbc"))
    implementation("com.ibm.db2:jcc:${DependencyVersions.db2Jcc}")
    api(commonDependencies.vertx.moduleWithoutVersion("db2-client")) // `api` used because `DB2Connection` has to be exposed
    implementation(commonDependencies.kotlinCommon.core()) // for `Untested`
    //implementation(commonDependencies.kotlinCommon.vertx()) // for `DB2PoolOptions.setUpConventionally`
}
