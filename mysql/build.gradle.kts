import com.huanshankeji.cpnProject

plugins {
    `lib-conventions`
}

dependencies {
    with(commonDependencies.vertx) { implementation(platformStackDepchain()) } // needed
    implementation(cpnProject(project, ":core"))

    runtimeOnly(commonDependencies.postgreSql()) // TODO change to the MySQL JDBC dependency
    implementation(commonDependencies.vertx.moduleWithoutVersion("mysql-client"))
    implementation(commonDependencies.kotlinCommon.core()) // for `Untested`
    implementation(commonDependencies.kotlinCommon.vertx()) // for `PgPoolOptions.setUpConventionally` // TODO check if this is necessary for MySQL, and remove if not
}