// TODO move to "kotlin-common"

package com.huanshankeji.jdbc

fun jdbcUrl(rdbms: String, host: String, port: Int?, database: String) =
    "jdbc:$rdbms://$host${port?.let { ":$it" } ?: ""}/$database"
