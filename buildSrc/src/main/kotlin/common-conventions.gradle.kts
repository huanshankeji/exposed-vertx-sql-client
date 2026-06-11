import com.huanshankeji.team.ShreckYe
import com.huanshankeji.team.setUpPomForTeamDefaultOpenSource

plugins {
    id("com.huanshankeji.team.with-group")
    id("maven-central")
    id("com.huanshankeji.team.default-github-packages-maven-publish")
    id("version")
    id("dokka-convention")
    id("com.huanshankeji.maven-central-publish-conventions")
}

mavenPublishing.pom {
    setUpPomForTeamDefaultOpenSource(
        project,
        "Exposed Vert.x SQL Client", "Exposed on top of Vert.x Reactive SQL Client", "2022"
    ) {
        ShreckYe()
    }
}
