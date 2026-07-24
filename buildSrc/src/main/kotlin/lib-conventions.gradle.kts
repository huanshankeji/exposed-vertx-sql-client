import com.huanshankeji.team.ShreckYe
import com.huanshankeji.team.setUpPomForTeamDefaultOpenSource
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    id("conventions")
    `java-library`
    id("com.huanshankeji.team.gitversioning.opensourceconvention.githubpackages.publish")
    id("com.huanshankeji.team.dokka.github-dokka-convention")
}

gitVersioningOpenSourceConventionGithubPackagesPublish {
    signAllPublicationsIfRelease(isRelease)
}

mavenPublishing.pom {
    setUpPomForTeamDefaultOpenSource(
        project,
        "Exposed Vert.x SQL Client", "Exposed on top of Vert.x Reactive SQL Client", "2022"
    ) {
        ShreckYe()
    }
}

kotlin {
    @OptIn(ExperimentalAbiValidation::class)
    abiValidation()
}
