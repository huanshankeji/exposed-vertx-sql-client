import com.huanshankeji.CommonDependencies
import com.huanshankeji.DefaultVersions

plugins {
    id("com.huanshankeji.kotlin-jvm-library-sonatype-ossrh-publish-conventions")
}

repositories {
    mavenLocal()
    mavenCentral()
}

java {
    registerFeature("postgresql") {
        usingSourceSet(sourceSets["main"])
    }
}

dependencies {
    api(CommonDependencies.Exposed.core())  // TODO: use `implementation` when possible
    // TODO: remove the Exposed JDBC dependency and the PostgresSQL dependency when there is no need to have an exposed transaction to generate SQLs
    runtimeOnly(CommonDependencies.Exposed.module("jdbc"))
    "postgresqlRuntimeOnly"("org.postgresql:postgresql:${DefaultVersions.postgreSql}")
    api(CommonDependencies.KotlinCommon.exposed())

    with(CommonDependencies.Vertx) {
        implementation(platformStackDepchain())
        api(moduleWithoutVersion("sql-client")) // TODO: use `implementation` when possible
        implementation(moduleWithoutVersion("lang-kotlin"))
        implementation(moduleWithoutVersion("lang-kotlin-coroutines"))
        "postgresqlImplementation"(moduleWithoutVersion("pg-client"))
    }
    implementation(CommonDependencies.KotlinCommon.vertx())

    implementation(CommonDependencies.KotlinCommon.core("0.1.1-kotlin-1.6.10-SNAPSHOT")) // TODO: don't use a SNAPSHOT version
    implementation(CommonDependencies.Arrow.core())

    implementation(CommonDependencies.KotlinCommon.net())
}

version = "0.1.0-kotlin-1.6.10-SNAPSHOT"

publishing.publications.getByName<MavenPublication>("maven") {
    artifactId = rootProject.name
}
