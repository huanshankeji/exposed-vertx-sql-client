package com.huanshankeji.exposedvertxsqlclient.jdbc

import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.jdbc.jdbcUrl

fun ConnectionConfig.Socket.jdbcUrl(rdbms: String) =
    jdbcUrl(rdbms, host, port, database)

/**
 * Generate JDBC URL for Unix domain socket connection for PostgreSQL.
 * Uses the junixsocket library format.
 * @param database the database name
 * @param socketPath the path to the Unix domain socket directory (e.g., "/var/run/postgresql").
 *                   The socket file `.s.PGSQL.5432` will be automatically appended.
 */
fun postgresqlUnixSocketJdbcUrl(database: String, socketPath: String) =
    "jdbc:postgresql://localhost/$database?socketFactory=org.newsclub.net.unix.AFUNIXSocketFactory\$FactoryArg&socketFactoryArg=$socketPath/.s.PGSQL.5432"

/**
 * Generate JDBC URL for Unix domain socket connection for MySQL.
 * Uses the junixsocket library format.
 * @param database the database name
 * @param socketPath the path to the Unix domain socket file. Should be the full path including the filename
 *                   (e.g., "/var/run/mysqld/mysqld.sock", "/tmp/mysql.sock", or "/var/lib/mysql/mysql.sock").
 *                   Note: The default MySQL socket path varies by distribution. Common paths include:
 *                   - Debian/Ubuntu: /var/run/mysqld/mysqld.sock
 *                   - RHEL/CentOS: /var/lib/mysql/mysql.sock
 *                   - macOS: /tmp/mysql.sock
 */
fun mysqlUnixSocketJdbcUrl(database: String, socketPath: String) =
    "jdbc:mysql://localhost/$database?socketFactory=org.newsclub.net.unix.AFUNIXSocketFactory\$FactoryArg&socketFactoryArg=$socketPath"
