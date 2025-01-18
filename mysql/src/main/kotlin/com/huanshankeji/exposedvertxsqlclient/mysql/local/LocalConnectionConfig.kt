package com.huanshankeji.exposedvertxsqlclient.mysql.local

import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.local.LocalConnectionConfig

// TODO consider moving to "kotlin-common"

const val DEFAULT_MYSQL_UNIX_DOMAIN_SOCKET_PATH = "/var/run/mysql"

@ExperimentalEvscApi
fun defaultMysqlLocalConnectionConfig(
    socketConnectionPort: Int? = null, user: String, socketConnectionPassword: String, database: String
) =
    LocalConnectionConfig(
        socketConnectionPort, DEFAULT_MYSQL_UNIX_DOMAIN_SOCKET_PATH, user, socketConnectionPassword, database
    )
