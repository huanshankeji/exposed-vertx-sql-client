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
    // With Kotlin 2.0.20, a "Could not parse POM" build error occurs in the JVM projects of some dependent projects.
    implementation(kotlin("gradle-plugin", "2.0.10"))
    implementation("com.huanshankeji:common-gradle-dependencies:0.7.2-20240916-SNAPSHOT") // TODO don't use a snapshot version in a main branch
    implementation("com.huanshankeji.team:gradle-plugins:0.5.1")
}
