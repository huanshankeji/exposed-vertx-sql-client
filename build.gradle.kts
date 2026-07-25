plugins {
    id("com.huanshankeji.root-project-conventions")
    id("org.jetbrains.dokka")
}

dependencies {
    for (project in subprojects.filterNot { it.name == "exposed-vertx-sql-client-integrated" })
        dokka(project)
}
