package com.huanshankeji.exposedvertxsqlclient

import com.huanshankeji.exposedvertxsqlclient.DatabaseConstants.SOCKET_HOST
import com.huanshankeji.exposedvertxsqlclient.DatabaseConstants.UNIX_DOMAIN_SOCKET_PATH
import com.huanshankeji.net.LOCALHOST

object DatabaseConstants {
    const val UNIX_DOMAIN_SOCKET_PATH = "/var/run/postgresql"
    const val SOCKET_HOST = LOCALHOST
}

class DatabaseConfig(val database: String, val user: String, val socketConnectionPassword: String) {
    val socketConnectionConfig by lazy {
        ConnectionConfig.Socket(SOCKET_HOST, user, socketConnectionPassword, database)
    }
    val unixDomainSocketWithPeerAuthenticationConnectionConfig by lazy {
        ConnectionConfig.UnixDomainSocketWithPeerAuthentication(UNIX_DOMAIN_SOCKET_PATH, user, database)
    }
}
