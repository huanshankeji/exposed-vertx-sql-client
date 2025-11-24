import com.huanshankeji.team.ShreckYe
import com.huanshankeji.team.setUpPomForTeamDefaultOpenSource

plugins {
    id("conventions")
    `java-library`
    id("com.huanshankeji.maven-central-publish-conventions")
    id("com.huanshankeji.team.default-github-packages-maven-publish")
    id("com.huanshankeji.team.dokka.github-dokka-convention")
    id("org.jetbrains.kotlinx.kover")
}

mavenPublishing.pom {
    setUpPomForTeamDefaultOpenSource(
        project,
        "Exposed Vert.x SQL Client", "Exposed on top of Vert.x Reactive SQL Client", "2022"
    ) {
        ShreckYe()
    }
}
