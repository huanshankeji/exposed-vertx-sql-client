package com.huanshankeji.exposedvertxsqlclient

@InternalApi
inline fun String.transformPreparedSqlToNumbered(appendNumberPrefix: StringBuilder.() -> StringBuilder): String =
    // twice capacity by default
    buildString(length * 2) {
        var i = 1
        for (c in this@transformPreparedSqlToNumbered)
            if (c == '?') appendNumberPrefix().append(i++)
            else append(c)
    }
