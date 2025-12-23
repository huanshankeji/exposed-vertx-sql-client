package com.huanshankeji.exposedvertxsqlclient.r2dbc

import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions.*
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig

// move to a new `r2dbc` module if these are proved useful

fun ConnectionConfig.Socket.r2dbcConnectionFactory() =
    ConnectionFactories.get(
        builder()
            .option(DRIVER, "postgresql")
            .option(HOST, host)
            .option(USER, user)
            .option(PASSWORD, password)
            .option(DATABASE, database)
            .build()
    )

fun ConnectionFactory.exposedR2dbcDatabaseConnectPostgresql() =
    R2dbcDatabase.connect(this, R2dbcDatabaseConfig {
        explicitDialect = PostgreSQLDialect()
    })

fun ConnectionConfig.Socket.exposedR2dbcDatabaseConnectPostgresql() =
    r2dbcConnectionFactory().exposedR2dbcDatabaseConnectPostgresql()

// add `extraConfig` if moved
fun ConnectionConfig.Socket.connectionPoolConfiguration(size: Int) =
    ConnectionPoolConfiguration.builder(r2dbcConnectionFactory())
        .initialSize(size)
        .maxSize(size)
        .build()

fun ConnectionConfig.Socket.connectionPool(size: Int) =
    ConnectionPool(connectionPoolConfiguration(size))

/*
// Don't call this because the connection pool need to be closed after use.
fun ConnectionConfig.Socket.exposedR2dbcDatabaseConnectPoolPostgresql(connectionPoolSize: Int) =
    R2dbcDatabase.connect(connectionPool(connectionPoolSize), R2dbcDatabaseConfig {
        explicitDialect = PostgreSQLDialect()
    })
*/
