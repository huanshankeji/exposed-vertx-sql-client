plugins {
    id("org.jetbrains.dokka")
    id("com.huanshankeji.root-project-conventions")
}

dependencies {
    for (project in subprojects.filterNot { it.name == "exposed-vertx-sql-client-integrated" })
        dokka(project)
}
