import com.huanshankeji.cpnProject

plugins {
    `lib-conventions`
}

dependencies {
    with(commonDependencies.vertx) { implementation(platformStackDepchain()) } // needed
    implementation(cpnProject(project, ":core"))

    runtimeOnly(commonDependencies.postgreSql())
    implementation(commonDependencies.vertx.moduleWithoutVersion("pg-client"))
}