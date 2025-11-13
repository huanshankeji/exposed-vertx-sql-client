package com.huanshankeji.exposedvertxsqlclient

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import kotlin.concurrent.thread

fun main() {
    val postgreSQLContainer = PostgreSQLContainer(DockerImageName.parse("postgres:latest"))
    postgreSQLContainer.start()

    repeat(2) {
        List(Runtime.getRuntime().availableProcessors()) {
            thread {
                val database = with(postgreSQLContainer) {
                    Database.connect(
                        "jdbc:${"postgresql"}://$host$firstMappedPort/$databaseName",
                        "org.postgresql.Driver",
                        username,
                        password
                    )
                }
                repeat(10_000) { transaction(database) {} }
            }
        }.forEach {
            it.join()
        }
    }
}
