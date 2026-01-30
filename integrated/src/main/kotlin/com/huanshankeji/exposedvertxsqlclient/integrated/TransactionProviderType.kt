package com.huanshankeji.exposedvertxsqlclient.integrated

import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi

/**
 * Enum representing different types of Exposed statement preparation transaction providers for testing purposes.
 * 
 * This enum is currently not actively used but is provided for potential future use
 * in parameterized testing or configuration scenarios. Note that this is about Exposed transactions
 * used for statement preparation, not Vert.x SQL client transactions.
 */
@ExperimentalEvscApi
enum class ExposedStatementPreparationTransactionProviderType {
    /** Uses DatabaseExposedTransactionProvider (creates Exposed transaction for each SQL preparation) */
    Database,
    /** Uses JdbcTransactionExposedTransactionProvider (reuses JDBC transaction) */
    JdbcTransaction
}
