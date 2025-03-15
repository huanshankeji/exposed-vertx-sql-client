package com.huanshankeji.exposedvertxsqlclient.postgresql

import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import org.jetbrains.exposed.sql.statements.Statement
import org.jetbrains.exposed.sql.Transaction as ExposedTransaction

@ExperimentalEvscApi
fun String.transformPgPreparedSql(): String =
    buildString(length * 2) {
        var i = 1
        for (c in this)
            if (c == '?') append('$').append(i++)
            else append(c)
    }

// TODO consider removing
// TODO context parameters
@ExperimentalEvscApi
fun Statement<*>.getVertxPgClientPreparedSql(transaction: ExposedTransaction) =
    prepareSQL(transaction).transformPgPreparedSql()
