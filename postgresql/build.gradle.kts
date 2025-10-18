import com.huanshankeji.cpnProject

plugins {
    `lib-conventions`
}

dependencies {
    with(commonDependencies.vertx) { implementation(platformStackDepchain()) } // needed
    implementation(cpnProject(project, ":core"))

    // TODO remove the Exposed JDBC dependency and the PostgresSQL dependency when there is no need to to generate SQLs with an Exposed transaction
    implementation(commonDependencies.exposed.module("jdbc"))
    runtimeOnly(commonDependencies.postgreSql())
    implementation(commonDependencies.vertx.moduleWithoutVersion("pg-client"))
    implementation(commonDependencies.kotlinCommon.core()) // for `Untested`
    //implementation(commonDependencies.kotlinCommon.vertx()) // for `PgPoolOptions.setUpConventionally`
}
