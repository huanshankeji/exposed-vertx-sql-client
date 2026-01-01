package com.huanshankeji.exposedvertxsqlclient.integrated

enum class RdbmsType {
    Postgresql, Mysql, Oracle, Mssql;

    companion object {
        val Sqlserver = Mssql
    }
}

typealias Dialect = RdbmsType
