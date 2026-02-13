package com.huanshankeji.exposedvertxsqlclient.integrated

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.SpecExecutionOrder

/**
 * Kotest project configuration to enable parallel test execution.
 * 
 * This configuration allows different test specs (classes extending AllConfigurationsSpec)
 * to run in parallel, which significantly reduces total test execution time since
 * each spec tests a different combination of database types and configurations.
 */
class ProjectConfig : AbstractProjectConfig() {
    // Enable parallel execution of different spec classes
    // Each spec (e.g., SimpleExamplesTests, TransactionTests) can run concurrently
    override val parallelism = Runtime.getRuntime().availableProcessors()
    
    // Run specs in a deterministic order for reproducibility
    override val specExecutionOrder = SpecExecutionOrder.Lexicographic
}
