import com.huanshankeji.cpnProject

plugins {
    `lib-conventions`
}

dependencies {
    with(commonDependencies.vertx) { implementation(platformStackDepchain()) } // needed
    implementation(cpnProject(project, ":core"))
    implementation(cpnProject(project, ":crud"))

    api(commonDependencies.kotlinCommon.exposed()) // for `updateBuilderSetter`, `DataQueryMapper` and `DataUpdateMapper`
}
