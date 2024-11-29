package com.huanshankeji.exposedvertxsqlclient.jdbc

import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.jdbc.jdbcUrl

fun ConnectionConfig.Socket.jdbcUrl(rdbms: String) =
    jdbcUrl(rdbms, host, port, database)
