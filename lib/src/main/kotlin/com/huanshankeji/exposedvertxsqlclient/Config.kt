package com.huanshankeji.exposedvertxsqlclient

import com.huanshankeji.net.LOCALHOST

class Config(val database: String, val user: String, val socketConnectionPassword: String) {
    companion object {
        const val UNIX_DOMAIN_SOCKET_PATH = "/var/run/postgresql"
        const val SOCKET_HOST = LOCALHOST
    }

    val socketConnectionConfig =
        ConnectionConfig.Socket(SOCKET_HOST, user, socketConnectionPassword, database)

    val unixDomainSocketWithPeerAuthenticationConnectionConfig =
        ConnectionConfig.UnixDomainSocketWithPeerAuthentication(UNIX_DOMAIN_SOCKET_PATH, user, database)
}

// mainly for code completion
@Deprecated("Use `Config` instead.", ReplaceWith("Config"))
typealias ExposedVertxSqlClientConfig = Config
