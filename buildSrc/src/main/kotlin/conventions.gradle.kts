import com.huanshankeji.team.`Shreck Ye`
import com.huanshankeji.team.pomForTeamDefaultOpenSource
import com.huanshankeji.team.repositoriesAddTeamGithubPackagesMavenRegistry
import org.gradle.kotlin.dsl.repositories

plugins {
    id("com.huanshankeji.team.with-group")
    id("com.huanshankeji.kotlin-jvm-library-sonatype-ossrh-publish-conventions")
    id("com.huanshankeji.team.default-github-packages-maven-publish")
}

repositories {
    mavenLocal()
    mavenCentral()
}
repositoriesAddTeamGithubPackagesMavenRegistry("kotlin-common")

kotlin.jvmToolchain(8)

version = projectVersion

publishing.publications.getByName<MavenPublication>("maven") {
    pomForTeamDefaultOpenSource(project, "Exposed Vert.x SQL Client", "Exposed on top of Vert.x Reactive SQL Client") {
        `Shreck Ye`()
    }
}
