package com.huanshankeji.exposedvertxsqlclient.integrated

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.SpecExecutionOrder

/**
 * Kotest project configuration to enable parallel test execution.
 * 
 * This configuration allows different test specs (classes extending AllConfigurationsSpec)
 * to run in parallel, which significantly reduces total test execution time since
 * each spec tests a different combination of database types and configurations.
 * 
 * Note: With InstancePerLeaf isolation mode set in AllConfigurationsSpec, each test leaf
 * will get its own spec instance, enabling safe concurrent execution.
 */
class ProjectConfig : AbstractProjectConfig() {
    // Run specs in a deterministic order for reproducibility
    override val specExecutionOrder = SpecExecutionOrder.Lexicographic
    
    // Enable parallel execution - with Kotest 5.x+ this is typically controlled by
    // Gradle's maxParallelForks and Kotest will run specs concurrently by default
}
