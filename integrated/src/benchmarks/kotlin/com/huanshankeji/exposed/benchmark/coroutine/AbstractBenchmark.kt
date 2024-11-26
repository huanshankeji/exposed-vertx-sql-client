package com.huanshankeji.exposed.benchmark.coroutine

import kotlinx.benchmark.Measurement
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import kotlinx.benchmark.Warmup

@State(Scope.Benchmark)
@Warmup(time = 1, iterations = 8)
@Measurement(time = 1, iterations = 8)
abstract class AbstractBenchmark