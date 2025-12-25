package com.huanshankeji.exposedvertxsqlclient.local

import com.huanshankeji.exposedvertxsqlclient.*
import com.huanshankeji.net.LOCALHOST

// TODO consider adding a prefix word such as "conventional" as this class is not general enough
// TODO consider refactoring this class into a function / functions to reduce the cognitive complexity
/**
 * A kind of connection config that can produce both a [ConnectionConfig.Socket] and a [ConnectionConfig.UnixDomainSocketWithPeerAuthentication]
 * to connect to a local database server.
 */
@ExperimentalEvscApi
@ExperimentalUnixDomainSocketApi
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
@ExperimentalUnixDomainSocketApi
fun LocalConnectionConfig.toPerformantUnixEvscConfig() =
    EvscConfig(socketConnectionConfig, unixDomainSocketWithPeerAuthenticationConnectionConfig)

/**
 * This can be used on OSs without support for Unix domain sockets such as Windows.
 */
@ExperimentalEvscApi
@ExperimentalUnixDomainSocketApi
fun LocalConnectionConfig.toUniversalEvscConfig() =
    EvscConfig(socketConnectionConfig, socketConnectionConfig)
