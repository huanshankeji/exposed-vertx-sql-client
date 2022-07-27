rootProject.name = "exposed-vertx-sql-client"
include("lib")

buildscript {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
    dependencies {
        classpath(kotlin("gradle-plugin", "1.7.10"))
        classpath("com.huanshankeji:kotlin-common-gradle-plugins:0.1.7-kotlin-1.6.10")
        classpath("com.huanshankeji:common-gradle-dependencies:0.2.0-20220720-kotlin-1.6.10")
    }
}
