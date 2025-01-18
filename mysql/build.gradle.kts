import com.huanshankeji.cpnProject

plugins {
    `lib-conventions`
}

dependencies {
    with(commonDependencies.vertx) { implementation(platformStackDepchain()) } // needed
    implementation(cpnProject(project, ":core"))

    // TODO remove the Exposed JDBC dependency and the MySQL dependency when there is no need to to generate SQLs with an Exposed transaction
    runtimeOnly(commonDependencies.exposed.module("jdbc"))
    implementation("com.mysql:mysql-connector-j:9.1.0")
    implementation(commonDependencies.vertx.moduleWithoutVersion("mysql-client"))
    implementation(commonDependencies.kotlinCommon.core()) // for `Untested`
    //implementation(commonDependencies.kotlinCommon.vertx()) // for `MySQLPoolOptions.setUpConventionally`
}
