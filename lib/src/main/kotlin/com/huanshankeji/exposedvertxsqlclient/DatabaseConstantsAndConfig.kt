package com.huanshankeji.exposedvertxsqlclient

import com.huanshankeji.exposedvertxsqlclient.DatabaseConstants.PASSWORD
import com.huanshankeji.exposedvertxsqlclient.DatabaseConstants.SOCKET_HOST
import com.huanshankeji.exposedvertxsqlclient.DatabaseConstants.UNIX_DOMAIN_SOCKET_PATH

object DatabaseConstants {
    const val UNIX_DOMAIN_SOCKET_PATH = "/var/run/postgresql"
    const val SOCKET_HOST = "localhost"
    const val PASSWORD = "password" // used for localhost only
}

abstract class DatabaseConfig {
    abstract val database: String
    abstract val user: String

    val socketConnectionConfig by lazy { ConnectionConfig.Socket(SOCKET_HOST, user, PASSWORD, database) }
    val unixDomainSocketWithPeerAuthenticationConnectionConfig by lazy {
        ConnectionConfig.UnixDomainSocketWithPeerAuthentication(UNIX_DOMAIN_SOCKET_PATH, user, database)
    }
}
