package com.huanshankeji.exposedvertxsqlclient.integrated

import com.huanshankeji.exposedvertxsqlclient.DatabaseExposedTransactionProvider
import com.huanshankeji.exposedvertxsqlclient.ReusingJdbcTransactionExposedTransactionProvider

/**
 * Enum representing different types of Exposed statement preparation transaction providers for testing purposes.
 */
enum class StatementPreparationExposedTransactionProviderType {
    /** Uses [DatabaseExposedTransactionProvider] (creates Exposed transaction for each SQL preparation) */
    Database,

    /** Uses [ReusingJdbcTransactionExposedTransactionProvider] (reuses JDBC transaction) */
    JdbcTransaction
}
