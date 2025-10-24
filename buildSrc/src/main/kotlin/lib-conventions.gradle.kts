import com.huanshankeji.team.ShreckYe
import com.huanshankeji.team.pomForTeamDefaultOpenSource

plugins {
    id("conventions")
    id("com.huanshankeji.kotlin-jvm-library-sonatype-ossrh-publish-conventions")
    id("com.huanshankeji.team.default-github-packages-maven-publish")
    id("com.huanshankeji.team.dokka.github-dokka-convention")
}

publishing.publications.getByName<MavenPublication>("maven") {
    pomForTeamDefaultOpenSource(project, "Exposed Vert.x SQL Client", "Exposed on top of Vert.x Reactive SQL Client") {
        ShreckYe()
    }
}
