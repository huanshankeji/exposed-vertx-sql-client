package com.huanshankeji.exposedvertxsqlclient.benchmark.tfb

import org.jetbrains.exposed.v1.core.dao.id.IdTable

// https://github.com/huanshankeji/FrameworkBenchmarks/blob/aa271b70ff99411c8a47e99a06cfa2d856245dd0/frameworks/Kotlin/vertx-web-kotlinx/with-db/exposed-common/src/main/kotlin/database/Tables.kt
object WorldTable : IdTable<Int>("world") {
    override val id = integer("id").entityId()

    // The name is "randomNumber" in "create-postgres.sql" but it's actually "randomnumber" in the test database.
    val randomNumber = integer("randomnumber").default(0)
}

// https://github.com/huanshankeji/FrameworkBenchmarks/blob/aa271b70ff99411c8a47e99a06cfa2d856245dd0/frameworks/Kotlin/vertx-web-kotlinx/common/src/main/kotlin/Models.kt
data class World(val id: Int, val randomNumber: Int)
