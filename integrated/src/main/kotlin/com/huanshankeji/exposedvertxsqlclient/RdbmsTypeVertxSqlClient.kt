package com.huanshankeji.exposedvertxsqlclient

import io.vertx.mssqlclient.MSSQLConnection
import io.vertx.mysqlclient.MySQLConnection
import io.vertx.oracleclient.OracleConnection
import io.vertx.pgclient.PgConnection

// not currently used
fun RdbmsType.getVertxSqlConnectionClass() =
    when (this) {
        RdbmsType.Postgresql -> PgConnection::class
        RdbmsType.Mysql -> MySQLConnection::class
        RdbmsType.Oracle -> OracleConnection::class
        RdbmsType.Mssql -> MSSQLConnection::class
    }
