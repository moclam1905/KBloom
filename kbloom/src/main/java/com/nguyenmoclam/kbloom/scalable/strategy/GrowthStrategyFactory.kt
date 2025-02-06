package com.nguyenmoclam.kbloom.scalable.strategy

object GrowthStrategyFactory {
    private val strategies = mapOf(
        "DefaultGrowthStrategy" to DefaultGrowthStrategy,
        "GeometricScalingGrowthStrategy" to GeometricScalingGrowthStrategy,
        "TighteningScalingGrowthStrategy" to TighteningScalingGrowthStrategy,

    )

    fun getStrategyByName(strategyName: String): GrowthStrategy {
        return strategies[strategyName]
            ?: throw IllegalArgumentException("Unknown strategy name: $strategyName")
    }

    fun getNameByStrategy(strategy: GrowthStrategy): String {
        return strategies.entries.find { it.value::class == strategy::class }?.key
            ?: throw IllegalArgumentException("Unknown strategy or Strategy not registered: $strategy")
    }

    fun registerStrategy(strategyName: String, strategy: GrowthStrategy) {
        strategies.plus(strategyName to strategy)
    }

    // Example: GrowthStrategyFactory.registerStrategy("CustomGrowthStrategy", CustomGrowthStrategy)
}
