package com.huanshankeji.exposedvertxsqlclient

import com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient.setRole

/**
 * Configuration for database connections, supporting both TCP socket and Unix domain socket connections.
 *
 * Use [ConnectionConfig.Socket] for standard TCP/IP connections, or [ConnectionConfig.UnixDomainSocketWithPeerAuthentication]
 * for Unix domain socket connections with peer authentication (experimental, primarily tested with PostgreSQL on Linux).
 *
 * @see Socket
 * @see UnixDomainSocketWithPeerAuthentication
 */
sealed interface ConnectionConfig {
    /** The user or role name used for authentication. */
    val userAndRole: String
    /** The database name to connect to. */
    val database: String

    /**
     * Standard TCP/IP socket connection configuration.
     *
     * @param host the database server hostname or IP address.
     * @param port the database server port, or `null` to use the default port for the database type.
     * @param user the username for authentication.
     * @param password the password for authentication.
     * @param database the name of the database to connect to.
     */
    class Socket(
        val host: String,
        val port: Int? = null, // `null` for the default port
        val user: String,
        val password: String,
        override val database: String
    ) : ConnectionConfig {
        override val userAndRole: String get() = user
    }

    /**
     * Unix domain socket connection configuration with peer authentication.
     * @see setRole
     */
    @ExperimentalUnixDomainSocketApi
    class UnixDomainSocketWithPeerAuthentication(
        val path: String,
        val role: String,
        override val database: String
    ) : ConnectionConfig {
        override val userAndRole: String get() = role
    }
}

/**
 * Converts this socket connection config to an [EvscConfig] using the same config for both Exposed and Vert.x SQL Client.
 * This is the simplest configuration for standard TCP/IP connections.
 */
fun ConnectionConfig.Socket.toUniversalEvscConfig() =
    EvscConfig(this, this)
