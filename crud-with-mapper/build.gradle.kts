import com.huanshankeji.cpnProject

plugins {
    `lib-conventions`
}

dependencies {
    with(commonDependencies.vertx) { implementation(platformStackDepchain()) } // needed
    implementation(cpnProject(project, ":core"))
    implementation(cpnProject(project, ":crud"))

    api(commonDependencies.kotlinCommon.exposed()) // `DataQueryMapper`, `DataUpdateMapper`, and `updateBuilderSetter`
    implementation(commonDependencies.kotlinCommon.core()) // `@ExperimentalApi` on those mapper interfaces
}
