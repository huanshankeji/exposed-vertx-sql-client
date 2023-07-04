plugins {
    `kotlin-dsl`
}

repositories {
    mavenLocal()
    gradlePluginPortal()
    maven {
        url = uri("https://maven.pkg.github.com/huanshankeji/gradle-common")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
        }
    }
}

dependencies {
    implementation(kotlin("gradle-plugin", "1.8.21"))
    // TODO: don't use snapshot versions in a main branch
    implementation("com.huanshankeji:common-gradle-dependencies:0.7.0-20230621-SNAPSHOT")
    implementation("com.huanshankeji.team:gradle-plugins:0.5.0-SNAPSHOT")
}
