package com.huanshankeji.exposedvertxsqlclient.local

import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.EvscConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.net.LOCALHOST

// TODO consider adding a prefix word such as "conventional" as this class is not general enough
// TODO consider refactoring this class into a function / functions to reduce the cognitive complexity
/**
 * A kind of connection config that can produce both a [ConnectionConfig.Socket] and a [ConnectionConfig.UnixDomainSocketWithPeerAuthentication]
 * to connect to a local database server.
 */
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

@ExperimentalEvscApi
fun LocalConnectionConfig.toPerformantUnixEvscConfig() =
    EvscConfig(socketConnectionConfig, unixDomainSocketWithPeerAuthenticationConnectionConfig)

@ExperimentalEvscApi
fun LocalConnectionConfig.toUniversalEvscConfig() =
    EvscConfig(socketConnectionConfig, socketConnectionConfig)
