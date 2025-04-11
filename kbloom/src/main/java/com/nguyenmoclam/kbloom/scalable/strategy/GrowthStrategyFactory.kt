package com.nguyenmoclam.kbloom.scalable.strategy

object GrowthStrategyFactory {
    // Use fully qualified class name as the key for better robustness
    private val strategies: MutableMap<String, GrowthStrategy> = mutableMapOf(
        DefaultGrowthStrategy::class.java.name to DefaultGrowthStrategy,
        GeometricScalingGrowthStrategy::class.java.name to GeometricScalingGrowthStrategy,
        TighteningScalingGrowthStrategy::class.java.name to TighteningScalingGrowthStrategy
    )

    /**
     * Retrieves a GrowthStrategy instance based on its fully qualified class name.
     * Returns null if the class name is not registered.
     *
     * @param className The fully qualified class name of the strategy.
     * @return The corresponding GrowthStrategy instance, or null if not found.
     */
    fun getStrategyByClassName(className: String): GrowthStrategy? {
        return strategies[className]
        // Consider adding reflection-based instantiation for custom strategies if needed,
        // but that adds complexity and potential ProGuard issues.
        // For now, only registered strategies are supported.
    }

    /**
     * Gets the fully qualified class name for a given GrowthStrategy instance.
     * Used for serialization/parcelization.
     *
     * @param strategy The GrowthStrategy instance.
     * @return The fully qualified class name, or throws an exception if the strategy is not registered.
     */
    fun getClassNameByStrategy(strategy: GrowthStrategy): String {
        // Find the key (class name) by matching the instance's class
        return strategies.entries.find { it.value::class == strategy::class }?.key
            ?: strategy::class.java.name // Fallback to reflection if not found (e.g., custom unregistered)
            // Consider throwing an exception if strict registration is required:
            // ?: throw IllegalArgumentException("Strategy class not registered in factory: ${strategy::class.java.name}")
    }

    /**
     * Registers a custom GrowthStrategy with the factory.
     * This allows custom strategies to be potentially restored if their class name is stored.
     *
     * @param strategy The GrowthStrategy instance to register. Its class name will be used as the key.
     */
    fun registerStrategy(strategy: GrowthStrategy) {
        strategies[strategy::class.java.name] = strategy
    }

    // Example: GrowthStrategyFactory.registerStrategy(CustomGrowthStrategy())
}
