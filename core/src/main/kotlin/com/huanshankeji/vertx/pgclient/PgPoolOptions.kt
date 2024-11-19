// TODO move to "kotlin-common"

package com.huanshankeji.vertx.pgclient

import io.vertx.pgclient.impl.PgPoolOptions

/**
 * Optimized for throughput.
 */
fun PgPoolOptions.setUpConventionally() {
    isPipelined = true
}
