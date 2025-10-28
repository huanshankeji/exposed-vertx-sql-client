import com.huanshankeji.team.repositoriesAddTeamGithubPackagesMavenRegistry
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("com.huanshankeji.team.with-group")
    kotlin("jvm")
}

repositories {
    mavenLocal()
    mavenCentral()
}
// commented out as it may slow down the build, especially when the GitHub token is incorrect and authentication fails
//repositoriesAddTeamGithubPackagesMavenRegistry("kotlin-common")

kotlin.jvmToolchain(11)

version = projectVersion

tasks.named<KotlinCompilationTask<*>>("compileKotlin").configure {
    compilerOptions.freeCompilerArgs.add("-opt-in=com.huanshankeji.exposedvertxsqlclient.InternalApi")
}
