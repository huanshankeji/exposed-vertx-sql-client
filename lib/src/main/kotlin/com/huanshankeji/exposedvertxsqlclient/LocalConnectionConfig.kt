package com.huanshankeji.exposedvertxsqlclient

import com.huanshankeji.net.LOCALHOST

class LocalConnectionConfig(val database: String, val user: String, val socketConnectionPassword: String) {
    companion object {
        const val UNIX_DOMAIN_SOCKET_PATH = "/var/run/postgresql"
        const val SOCKET_HOST = LOCALHOST
    }

    val socketConnectionConfig =
        ConnectionConfig.Socket(SOCKET_HOST, null, user, socketConnectionPassword, database)

    val unixDomainSocketWithPeerAuthenticationConnectionConfig =
        ConnectionConfig.UnixDomainSocketWithPeerAuthentication(UNIX_DOMAIN_SOCKET_PATH, user, database)
}
