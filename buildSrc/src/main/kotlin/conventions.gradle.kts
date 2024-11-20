import com.huanshankeji.team.repositoriesAddTeamGithubPackagesMavenRegistry

plugins {
    id("com.huanshankeji.team.with-group")
    kotlin("jvm")
}

repositories {
    mavenLocal()
    mavenCentral()
}
repositoriesAddTeamGithubPackagesMavenRegistry("kotlin-common")

kotlin.jvmToolchain(8)

version = projectVersion
