package com.huanshankeji.exposedvertxsqlclient

@ExperimentalEvscApi
//@ExperimentalUnixDomainSocketApi
interface IEvscConfig {
    val exposedConnectionConfig: ConnectionConfig.Socket
    val vertxSqlClientConnectionConfig: ConnectionConfig
}

// TODO add a type parameter for `exposedConnectionConfig` to better support RDBMSs that don't support Unix domain sockets
/**
 * This API is not used in the factory function parameter types yet.
 */
@ExperimentalEvscApi
//@ExperimentalUnixDomainSocketApi
class EvscConfig(
    override val exposedConnectionConfig: ConnectionConfig.Socket,
    override val vertxSqlClientConnectionConfig: ConnectionConfig
) : IEvscConfig
