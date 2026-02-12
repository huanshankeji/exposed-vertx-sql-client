package com.huanshankeji.exposedvertxsqlclient

@EvscInternalApi
val unquotedSqlIdentifierRegex = Regex("\\w+")

/**
 * To prevent SQL injection. Does not take into account quoted identifiers yet.
 */
@EvscInternalApi
fun requireSqlIdentifier(identifier: String) =
    require(identifier.matches(unquotedSqlIdentifierRegex)) {
        "Invalid SQL identifier '$identifier'. Only alphanumeric characters and underscores are allowed."
    }
