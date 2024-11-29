package com.huanshankeji.exposed.benchmark

import kotlinx.benchmark.Measurement
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import kotlinx.benchmark.Warmup

@State(Scope.Benchmark)
@Warmup(time = 1, iterations = 2)
@Measurement(time = 1, iterations = 2)
abstract class AbstractBenchmark