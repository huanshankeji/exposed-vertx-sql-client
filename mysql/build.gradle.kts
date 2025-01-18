import com.huanshankeji.cpnProject

plugins {
    `lib-conventions`
}

dependencies {
    with(commonDependencies.vertx) { implementation(platformStackDepchain()) } // needed
    implementation(cpnProject(project, ":core"))
    implementation("com.mysql:mysql-connector-j:9.1.0")
    // TODO remove the Exposed JDBC dependency and the PostgresSQL dependency when there is no need to to generate SQLs with an Exposed transaction
    runtimeOnly(commonDependencies.exposed.module("jdbc"))
    implementation(commonDependencies.vertx.moduleWithoutVersion("mysql-client"))
    implementation(commonDependencies.kotlinCommon.core()) // for `Untested`
    // implementation(commonDependencies.kotlinCommon.vertx()) // for `PgPoolOptions.setUpConventionally`
    // this seems to be needed as mentioned in vertx-mysql-client
}
