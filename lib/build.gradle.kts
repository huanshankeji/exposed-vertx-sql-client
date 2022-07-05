import com.huanshankeji.CommonDependencies
import com.huanshankeji.DefaultVersions

plugins {
    id("com.huanshankeji.kotlin-jvm-library-sonatype-ossrh-publish-conventions")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    api(CommonDependencies.Exposed.core())  // TODO: use `implementation` when possible
    // TODO: remove the Exposed JDBC dependency and the PostgresSQL dependency when there is no need to have an exposed transaction to generate SQLs
    runtimeOnly(CommonDependencies.Exposed.module("jdbc"))
    api(CommonDependencies.KotlinCommon.exposed())

    with(CommonDependencies.Vertx) {
        implementation(platformStackDepchain())
        api(moduleWithoutVersion("sql-client")) // TODO: use `implementation` when possible
        implementation(moduleWithoutVersion("lang-kotlin"))
        implementation(moduleWithoutVersion("lang-kotlin-coroutines"))
    }
    implementation(CommonDependencies.KotlinCommon.vertx())

    implementation(CommonDependencies.KotlinCommon.core())
    implementation(CommonDependencies.Arrow.core())

    implementation(CommonDependencies.KotlinCommon.net())
}

// for PostgreSQL
dependencies {
    runtimeOnly("org.postgresql:postgresql:${DefaultVersions.postgreSql}")
    implementation(CommonDependencies.Vertx.moduleWithoutVersion("pg-client"))
}

version = "0.1.1-kotlin-1.6.10-SNAPSHOT"

publishing.publications.getByName<MavenPublication>("maven") {
    artifactId = rootProject.name + "-postgresql"

    pom {
        name.set("Exposed Vert.x SQL Client")
        description.set("Exposed on top of Vert.x Reactive SQL Client")
        val githubUrl = "https://github.com/huanshankeji/exposed-vertx-sql-client"
        url.set(githubUrl)

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("ShreckYe")
                name.set("Shreck Ye")
                email.set("ShreckYe@gmail.com")
            }
        }
        scm {
            val scmString = "scm:git:$githubUrl.git"
            connection.set(scmString)
            developerConnection.set(scmString)
            url.set(githubUrl)
        }
    }
}
