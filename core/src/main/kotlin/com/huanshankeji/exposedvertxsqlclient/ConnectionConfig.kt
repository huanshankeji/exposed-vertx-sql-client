package com.huanshankeji.exposedvertxsqlclient

import com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient.setRole

sealed interface ConnectionConfig {
    val userAndRole: String
    val database: String

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

fun ConnectionConfig.Socket.toUniversalEvscConfig() =
    EvscConfig(this, this)
