package com.huanshankeji.exposedvertxsqlclient

import org.jetbrains.exposed.sql.Database

fun exposedDatabaseConnectPostgreSql(socketConnectionConfig: ConnectionConfig.Socket) =
    with(socketConnectionConfig) {
        Database.connect(
            "jdbc:postgresql://$host${port?.let { ":$it" } ?: ""}/$database",
            "org.postgresql.Driver",
            user = user,
            password = password
        )
    }
