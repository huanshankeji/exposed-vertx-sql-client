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
repositoriesAddTeamGithubPackagesMavenRegistry("kotlin-common")

kotlin.jvmToolchain(8)

version = projectVersion

tasks.named<KotlinCompilationTask<*>>("compileKotlin").configure {
    compilerOptions.freeCompilerArgs.add("-opt-in=com.huanshankeji.exposedvertxsqlclient.InternalApi")
}
