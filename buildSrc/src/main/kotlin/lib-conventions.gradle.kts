import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    id("common-conventions")
    id("conventions")
    `java-library`
}

kotlin {
    @OptIn(ExperimentalAbiValidation::class)
    abiValidation()
}
