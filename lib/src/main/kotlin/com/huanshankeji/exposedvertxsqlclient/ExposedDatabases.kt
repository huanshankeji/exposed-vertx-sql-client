package com.huanshankeji.exposedvertxsqlclient

import org.jetbrains.exposed.sql.Database

fun exposedDatabaseConnectPostgreSql(socketConnectionConfig: ConnectionConfig.Socket) =
    with(socketConnectionConfig) {
        Database.connect(
            "jdbc:postgresql://$host/$database",
            "org.postgresql.Driver",
            user = user,
            password = password
        )
    }
