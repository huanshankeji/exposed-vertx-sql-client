rootProject.name = "exposed-vertx-sql-client"
include("lib")

buildscript {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
    dependencies {
        classpath(kotlin("gradle-plugin", "1.7.10"))
        classpath("com.huanshankeji:common-gradle-dependencies:0.3.1-20220728")
        classpath("com.huanshankeji:kotlin-common-gradle-plugins:0.1.9")
    }
}
