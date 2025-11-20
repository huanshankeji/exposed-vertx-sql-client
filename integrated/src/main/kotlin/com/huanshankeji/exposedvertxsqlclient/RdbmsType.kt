package com.huanshankeji.exposedvertxsqlclient

enum class RdbmsType {
    Posgresql, Mysql, Oracle, Mssql;

    companion object {
        val Sqlserver = Mssql
    }
}

typealias Dialect = RdbmsType
