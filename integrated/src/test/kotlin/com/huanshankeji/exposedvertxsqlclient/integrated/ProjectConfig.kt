package com.huanshankeji.exposedvertxsqlclient.integrated

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.SpecExecutionOrder

/**
 * Kotest project configuration for the integrated test module.
 * 
 * This configuration provides project-level settings for test execution.
 * Parallel execution is enabled via Gradle's `maxParallelForks` setting in build.gradle.kts,
 * combined with `IsolationMode.InstancePerLeaf` in AllConfigurationsSpec which ensures
 * each test gets its own spec instance with isolated resources.
 */
class ProjectConfig : AbstractProjectConfig() {
    // Run specs in a deterministic order for reproducibility
    override val specExecutionOrder = SpecExecutionOrder.Lexicographic
}
