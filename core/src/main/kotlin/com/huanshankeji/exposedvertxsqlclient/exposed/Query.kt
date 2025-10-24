package com.huanshankeji.exposedvertxsqlclient.exposed

import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun Query.forUpdateWithTransaction() =
    transaction { forUpdate() }
