package com.huanshankeji.exposedvertxsqlclient.exposed

import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

// an alternative approach to the `buildQuery` variants
fun Query.forUpdateInNewTransaction() =
    transaction { forUpdate() }
