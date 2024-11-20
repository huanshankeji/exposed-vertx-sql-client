package com.huanshankeji.exposed.benchmark

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State

@State(Scope.Benchmark)
class EmptyBenchmark : AbstractBenchmark() {
    // for comparison
    @Benchmark
    fun empty() {
    }
}