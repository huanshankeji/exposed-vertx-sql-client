import com.huanshankeji.cpnProject

plugins {
    `lib-conventions`
}

dependencies {
    with(commonDependencies.vertx) { implementation(platformStackDepchain()) } // needed
    implementation(cpnProject(project, ":core"))
    implementation(cpnProject(project, ":crud"))

    implementation("com.huanshankeji:exposed-gadt-mapping:${DependencyVersions.exposedGadtMapping}") // for `updateBuilderSetter`, `DataQueryMapper` and `DataUpdateMapper`
}
