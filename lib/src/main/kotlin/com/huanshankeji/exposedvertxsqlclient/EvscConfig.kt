package com.huanshankeji.exposedvertxsqlclient

@ExperimentalEvscApi
interface IEvscConfig {
    val exposedConnectionConfig: ConnectionConfig.Socket
    val vertxSqlClientConnectionConfig: ConnectionConfig
}

/**
 * This API is not used in the factory function parameter types yet. TODO
 */
@ExperimentalEvscApi
class EvscConfig(
    override val exposedConnectionConfig: ConnectionConfig.Socket,
    override val vertxSqlClientConnectionConfig: ConnectionConfig
) : IEvscConfig
