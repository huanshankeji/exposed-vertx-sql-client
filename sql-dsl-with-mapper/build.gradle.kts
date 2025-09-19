import com.huanshankeji.cpnProject

plugins {
    `lib-conventions`
}

dependencies {
    with(commonDependencies.vertx) { implementation(platformStackDepchain()) } // needed
    implementation(cpnProject(project, ":core"))
    implementation(cpnProject(project, ":sql-dsl"))

    implementation("com.huanshankeji:exposed-gadt-mapping:${DependencyVersions.exposedAdtMapping}") // for `updateBuilderSetter`, `DataQueryMapper` and `DataUpdateMapper`
}
