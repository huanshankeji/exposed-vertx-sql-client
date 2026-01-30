package com.huanshankeji.exposedvertxsqlclient.integrated

import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi

/**
 * Enum representing different types of transaction providers for testing purposes.
 * 
 * This enum is currently not actively used but is provided for potential future use
 * in parameterized testing or configuration scenarios.
 */
@ExperimentalEvscApi
enum class TransactionProviderType {
    /** Uses DatabaseExposedTransactionProvider (creates transaction for each SQL preparation) */
    Database,
    /** Uses SharedJdbcTransactionExposedTransactionProvider (reuses shared transaction) */
    SharedJdbcTransaction
}
