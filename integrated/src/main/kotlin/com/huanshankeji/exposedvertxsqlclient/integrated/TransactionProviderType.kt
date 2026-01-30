package com.huanshankeji.exposedvertxsqlclient.integrated

enum class TransactionProviderType {
    /** Uses DatabaseExposedTransactionProvider (creates transaction for each SQL preparation) */
    Database,
    /** Uses SharedJdbcTransactionExposedTransactionProvider (reuses shared transaction) */
    SharedJdbcTransaction
}
