package com.huanshankeji.exposedvertxsqlclient.postgresql

import com.huanshankeji.exposedvertxsqlclient.DatabaseClientConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.StatementPreparationExposedTransactionProvider
import org.jetbrains.exposed.v1.jdbc.Database
import java.sql.Connection

/**
 * See the [DatabaseClientConfig] interface for parameter descriptions.
 * 
 * @param exposedDatabase the Exposed [Database] to use for creating the transaction provider
 * @param statementPreparationExposedTransactionProvider optional custom provider; if not specified,
 *   creates a JdbcTransactionExposedTransactionProvider using [exposedDatabase]
 */
fun PgDatabaseClientConfig(
    exposedDatabase: Database,
    validateBatch: Boolean = true,
    logSql: Boolean = false,
    @Suppress("DEPRECATION")
    statementPreparationExposedTransactionIsolationLevel: Int? = Connection.TRANSACTION_READ_UNCOMMITTED,
    autoExposedTransaction: Boolean = false,
    statementPreparationExposedTransactionProvider: StatementPreparationExposedTransactionProvider? = null
) =
    @OptIn(ExperimentalEvscApi::class)
    DatabaseClientConfig(
        validateBatch,
        logSql,
        statementPreparationExposedTransactionIsolationLevel,
        autoExposedTransaction,
        statementPreparationExposedTransactionProvider,
        exposedDatabase,
        String::transformPgPreparedSql
    )
