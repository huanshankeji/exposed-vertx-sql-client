import com.huanshankeji.gitversioning.projectVersionFromGitProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("com.huanshankeji.team.with-group")
    kotlin("jvm")
}

kotlin.jvmToolchain(11)

version = providers.projectVersionFromGitProvider(projectBaseVersion).get()

// configure for all source sets
tasks.withType<KotlinCompilationTask<*>> {
    compilerOptions.optIn.addAll(
        "com.huanshankeji.exposedvertxsqlclient.EvscInternalApi",
        "com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi"
    )
}
