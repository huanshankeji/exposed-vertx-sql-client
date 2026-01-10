package com.huanshankeji.exposedvertxsqlclient

import org.jetbrains.exposed.v1.jdbc.Database

// TODO rename the interface to `EvscConfig` and the implementation to `EvscConfigImpl`

/**
 * Note that this API is experimental and subject to change.
 *
 * Interface for Exposed Vert.x SQL Client configuration that holds connection configs
 * for both Exposed (JDBC-based, for SQL generation) and Vert.x SQL Client (for reactive query execution).
 *
 * This allows using different connection types for each: for example, using a socket connection
 * for Exposed while using a Unix domain socket for Vert.x SQL Client for better performance on Linux.
 *
 * @see EvscConfig
 */
@ExperimentalEvscApi
//@ExperimentalUnixDomainSocketApi
interface IEvscConfig {
    /** The connection config for the Exposed [Database], used for SQL generation. */
    val exposedConnectionConfig: ConnectionConfig.Socket

    /** The connection config for the Vert.x SQL Client, used for reactive query execution. */
    val vertxSqlClientConnectionConfig: ConnectionConfig
}

// TODO add a type parameter for `exposedConnectionConfig` to better support RDBMSs that don't support Unix domain sockets
/**
 * Note that this API is experimental and subject to change.
 *
 * Default implementation of [IEvscConfig] that serves as the single source of truth for database connection configuration.
 *
 * For simple setups where both Exposed and Vert.x use the same TCP socket connection,
 * use [ConnectionConfig.Socket.toUniversalEvscConfig].
 *
 * For optimized setups on Linux, you can configure Exposed to use a socket connection (for JDBC compatibility)
 * while Vert.x uses a Unix domain socket for better performance.
 *
 * @param exposedConnectionConfig the socket connection config for Exposed SQL generation.
 * @param vertxSqlClientConnectionConfig the connection config for Vert.x SQL Client execution.
 */
@ExperimentalEvscApi
//@ExperimentalUnixDomainSocketApi
class EvscConfig(
    override val exposedConnectionConfig: ConnectionConfig.Socket,
    override val vertxSqlClientConnectionConfig: ConnectionConfig
) : IEvscConfig
