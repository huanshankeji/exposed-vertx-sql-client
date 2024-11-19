package com.huanshankeji.exposedvertxsqlclient.exposed

import org.jetbrains.exposed.sql.Database

// TODO remove
interface ExposedDatabaseFactory {
    fun databaseConnect(): Database
}