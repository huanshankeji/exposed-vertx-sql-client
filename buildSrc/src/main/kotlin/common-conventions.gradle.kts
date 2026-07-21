import com.huanshankeji.team.ShreckYe
import com.huanshankeji.team.setUpPomForTeamDefaultOpenSource

plugins {
    id("com.huanshankeji.team.with-group")
    id("com.huanshankeji.team.gitversioning.opensourceconvention.githubpackages.publish")
    id("dokka-convention")
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
