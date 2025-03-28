package com.huanshankeji.exposedvertxsqlclient

interface DatabaseClientConfig {
    val validateBatch: Boolean
    val logSql: Boolean

    /**
     * Transform Exposed's prepared SQL to Vert.x SQL Client's prepared SQL.
     */
    fun transformPreparedSql(exposedPreparedSql: String): String
}

inline fun DatabaseClientConfig(
    // TODO consider adding a `isProduction` parameter whose default depends on the runtime
    validateBatch: Boolean = true,
    logSql: Boolean = false,
    crossinline exposedPreparedSqlToVertxSqlClientPreparedSql: (preparedSql: String) -> String
): DatabaseClientConfig {
    return object : DatabaseClientConfig {
        override val validateBatch: Boolean = validateBatch
        override val logSql: Boolean = logSql
        override fun transformPreparedSql(exposedPreparedSql: String): String =
            exposedPreparedSqlToVertxSqlClientPreparedSql(exposedPreparedSql)
    }
}
