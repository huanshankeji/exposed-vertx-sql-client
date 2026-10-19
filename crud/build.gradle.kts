import com.huanshankeji.cpnProject

plugins {
    `lib-conventions`
}

dependencies {
    implementation(cpnProject(project, ":core"))

    compileOnly(commonDependencies.kotlinCommon.vertx()) // for `sortDataAndExecuteBatch`
}
