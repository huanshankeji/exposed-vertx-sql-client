package com.huanshankeji.exposedvertxsqlclient

@ExperimentalEvscApi
interface IEvscConfig {
    val exposedConnectionConfig: ConnectionConfig
    val vertxSqlClientConnectionConfig: ConnectionConfig
}

/**
 * Configuration for both Exposed (JDBC) and Vert.x SQL Client connections.
 * 
 * Both connection configs can be either Socket or UnixDomainSocketWithPeerAuthentication,
 * allowing for flexible configuration including using Unix domain sockets for Exposed connections.
 */
@ExperimentalEvscApi
class EvscConfig(
    override val exposedConnectionConfig: ConnectionConfig,
    override val vertxSqlClientConnectionConfig: ConnectionConfig
) : IEvscConfig
