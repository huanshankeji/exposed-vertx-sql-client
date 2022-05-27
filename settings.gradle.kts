rootProject.name = "exposed-vertx-sql-client"
include("lib")

buildscript {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.huanshankeji:kotlin-common-gradle-plugins:0.1.4-kotlin-1.6.10-SNAPSHOT") // TODO: don't use a SNAPSHOT version
        classpath("com.huanshankeji:common-gradle-dependencies:0.1.1-SNAPSHOT-20220522-dev-kotlin-1.6.10") // TODO: don't use a SNAPSHOT version
    }
}
