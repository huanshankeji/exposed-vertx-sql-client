package com.huanshankeji.exposedvertxsqlclient.local

import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.ConnectionType
import com.huanshankeji.exposedvertxsqlclient.EvscConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.net.LOCALHOST

// TODO consider adding a prefix word such as "conventional" as this class is not general enough
// TODO consider refactoring this class into a function / functions to reduce the cognitive complexity
/**
 * A kind of connection config that can produce both a [ConnectionConfig.Socket] and a [ConnectionConfig.UnixDomainSocketWithPeerAuthentication]
 * to connect to a local database server.
 */
@ExperimentalEvscApi
class LocalConnectionConfig(
    val socketConnectionPort: Int? = null,
    val unixDomainSocketPath: String,
    val user: String,
    val socketConnectionPassword: String,
    val database: String
) {
    companion object {
        const val SOCKET_HOST = LOCALHOST
    }

    val socketConnectionConfig =
        ConnectionConfig.Socket(SOCKET_HOST, socketConnectionPort, user, socketConnectionPassword, database)

    val unixDomainSocketWithPeerAuthenticationConnectionConfig =
        ConnectionConfig.UnixDomainSocketWithPeerAuthentication(unixDomainSocketPath, user, database)

    fun getConnectionConfig(connectionType: ConnectionType) =
        when (connectionType) {
            ConnectionType.Socket -> socketConnectionConfig
            ConnectionType.UnixDomainSocketWithPeerAuthentication -> unixDomainSocketWithPeerAuthenticationConnectionConfig
        }
}

@ExperimentalEvscApi
fun LocalConnectionConfig.toPerformantUnixEvscConfig() =
    EvscConfig(socketConnectionConfig, unixDomainSocketWithPeerAuthenticationConnectionConfig)

@ExperimentalEvscApi
fun LocalConnectionConfig.toUniversalEvscConfig() =
    EvscConfig(socketConnectionConfig, socketConnectionConfig)
