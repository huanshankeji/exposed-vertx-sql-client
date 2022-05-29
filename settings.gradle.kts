rootProject.name = "exposed-vertx-sql-client"
include("lib")

buildscript {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.huanshankeji:kotlin-common-gradle-plugins:0.1.5-kotlin-1.6.10")
        classpath("com.huanshankeji:common-gradle-dependencies:0.1.1-20220527-kotlin-1.6.10")
    }
}
