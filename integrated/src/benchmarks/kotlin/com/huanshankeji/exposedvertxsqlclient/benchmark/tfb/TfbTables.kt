package com.huanshankeji.exposedvertxsqlclient.benchmark.tfb

import org.jetbrains.exposed.v1.core.Table

// https://github.com/huanshankeji/FrameworkBenchmarks/blob/aa271b70ff99411c8a47e99a06cfa2d856245dd0/frameworks/Kotlin/vertx-web-kotlinx/with-db/exposed-common/src/main/kotlin/database/Tables.kt
object WorldTable : Table("world") {
    val id = integer("id")
    val randomNumber = integer("randomnumber")

    override val primaryKey = PrimaryKey(id)
}

// https://github.com/huanshankeji/FrameworkBenchmarks/blob/aa271b70ff99411c8a47e99a06cfa2d856245dd0/frameworks/Kotlin/vertx-web-kotlinx/common/src/main/kotlin/Models.kt
data class World(val id: Int, val randomNumber: Int)
