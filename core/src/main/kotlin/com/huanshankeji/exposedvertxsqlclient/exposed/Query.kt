package com.huanshankeji.exposedvertxsqlclient.exposed

import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun Query.forUpdateWithExposedTransaction() =
    transaction { forUpdate() }

@Deprecated(
    "Use `forUpdateWithExposedTransaction` instead.",
    ReplaceWith(
        "forUpdateWithExposedTransaction()",
        "com.huanshankeji.exposedvertxsqlclient.exposed.forUpdateWithExposedTransaction"
    )
)
fun Query.forUpdateWithTransaction() =
    forUpdateWithExposedTransaction()
