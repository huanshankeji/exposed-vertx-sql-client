plugins {
    `kotlin-dsl`
}

repositories {
    mavenLocal()
    gradlePluginPortal()
}

val gradleCommonPluginsVersion =
    "0.12.0-dev-commit-916286b26dcfaec3636e27447385c96da6d8ca92"

dependencies {
    implementation(kotlin("gradle-plugin", "2.4.0"))
    implementation("com.huanshankeji:common-gradle-dependencies:0.10.0-20251024")
    implementation("com.huanshankeji.team:project-gradle-plugins:$gradleCommonPluginsVersion")
    implementation("com.huanshankeji:kotlin-common-project-gradle-plugins:$gradleCommonPluginsVersion")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:2.2.0")
}
