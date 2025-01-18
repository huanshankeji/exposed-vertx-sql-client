package com.huanshankeji.exposedvertxsqlclient.mysql.local

import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.local.LocalConnectionConfig

// TODO consider moving to "kotlin-common"

const val DEFAULT_POSTGRESQL_UNIX_DOMAIN_SOCKET_PATH = "/var/run/postgresql"

@ExperimentalEvscApi
fun defaultPostgresqlLocalConnectionConfig(
    socketConnectionPort: Int? = null, user: String, socketConnectionPassword: String, database: String
) =
    LocalConnectionConfig(
        socketConnectionPort, DEFAULT_POSTGRESQL_UNIX_DOMAIN_SOCKET_PATH, user, socketConnectionPassword, database
    )
