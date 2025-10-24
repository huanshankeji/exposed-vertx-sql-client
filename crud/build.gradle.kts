import com.huanshankeji.cpnProject

plugins {
    `lib-conventions`
}

dependencies {
    with(commonDependencies.vertx) { implementation(platformStackDepchain()) } // needed
    implementation(cpnProject(project, ":core"))

    compileOnly(commonDependencies.kotlinCommon.vertx()) // for `sortDataAndExecuteBatch`
}
