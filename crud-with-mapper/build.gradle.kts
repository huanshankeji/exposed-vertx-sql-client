import com.huanshankeji.cpnProject

plugins {
    `lib-conventions`
}

dependencies {
    implementation(cpnProject(project, ":core"))
    implementation(cpnProject(project, ":crud"))

    implementation("com.huanshankeji:exposed-gadt-mapping:${DependencyVersions.exposedGadtMapping}") // for `updateBuilderSetter`, `DataQueryMapper` and `DataUpdateMapper`
}
