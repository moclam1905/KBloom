package com.nguyenmoclam.kbloom

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nguyenmoclam.kbloom.configuration.BloomFilterBuilder
import com.nguyenmoclam.kbloom.core.BloomFilter
import com.nguyenmoclam.kbloom.databinding.ActivityMetricsDetailBinding
import com.nguyenmoclam.kbloom.hashing.MurmurHash3
import com.nguyenmoclam.kbloom.monitoring.BloomFilterMetrics
import com.nguyenmoclam.kbloom.utils.OptimalCalculations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.text.*

@SuppressLint("SetTextI18n")
class MetricsDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMetricsDetailBinding
    private lateinit var bloomFilter: BloomFilter<String>
    private lateinit var metrics: BloomFilterMetrics<String>

    companion object {
        const val EXTRA_BLOOM_FILTER = "extra_bloom_filter"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMetricsDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val serializedFilter = intent.getByteArrayExtra(EXTRA_BLOOM_FILTER)
        if (serializedFilter == null) {
            finish()
            return
        }

        bloomFilter = BloomFilter.deserialize(
            byteArray = serializedFilter,
            hashFunction = MurmurHash3,
            toBytes = { it.toByteArray(Charsets.UTF_8) }
        )
        metrics = BloomFilterMetrics(bloomFilter)

        setupMetricsDisplay()
        setupTestSection()
    }

    @SuppressLint("DefaultLocale")
    private fun setupMetricsDisplay() {
        with(binding) {
            // Memory Usage Section
            val memoryKB = metrics.getMemoryUsage() / 1024.0
            memoryUsageText.text = "Memory Usage: ${String.format("%.2f", memoryKB)} KB"
            memoryGuideText.text = "Recommended: < 1MB for mobile apps"

            // FPP Section
            val fpp = metrics.getCurrentFPP() * 100
            fppText.text = "False Positive Rate: ${String.format("%.4f", fpp)}%"
            fppGuideText.text = "Recommended: < 1% for most applications"

            // Fill Ratio Section
            val fillRatio = metrics.getFillRatio() * 100
            fillRatioText.text = "Fill Ratio: ${String.format("%.2f", fillRatio)}%"
            fillRatioGuideText.text = "Optimal: 30-50% for standard Bloom Filter"

            // Capacity Section
            val maxCapacity = OptimalCalculations.estimateMaxCapacity(
                bloomFilter.getBitSetSize(),
                bloomFilter.getFpp()
            )
            capacityText.text = "Max Recommended Capacity: $maxCapacity elements"
            capacityGuideText.text = "Current: ${metrics.getInsertedElements()} elements"
        }
    }

    private fun setupTestSection() {
        binding.runTestButton.setOnClickListener {
            lifecycleScope.launch {
                val testResults = runPerformanceTest()
                updateTestResults(testResults)
            }
        }
    }

    private suspend fun runPerformanceTest() = withContext(Dispatchers.Default) {
        val testSize = 1000
        var falsePositives = 0

        val testFilter = BloomFilterBuilder<String>()
            .expectedInsertions(bloomFilter.getBitSetSize())
            .falsePositiveProbability(bloomFilter.getFpp())
            .hashFunction(MurmurHash3)
            .toBytes { it.toByteArray(Charsets.UTF_8) }
            .build()

        val startTime = System.nanoTime()
        repeat(testSize) {
            testFilter.put("test_element_$it")
        }
        val insertionTime = (System.nanoTime() - startTime) / 1_000_000.0 // Convert to ms
    
        // Test for false positives
        val lookupStartTime = System.nanoTime()
        repeat(testSize) {
            if (testFilter.mightContain("non_existent_$it")) {
                falsePositives++
            }
        }
        val lookupTime = (System.nanoTime() - lookupStartTime) / 1_000_000.0 // Convert to ms
    
        val actualFpp = falsePositives.toDouble() / testSize
        TestResults(
            insertedCount = testSize,
            falsePositives = falsePositives,
            actualFpp = actualFpp,
            avgInsertionTimeMs = insertionTime / testSize,
            avgLookupTimeMs = lookupTime / testSize
        )
    }
    
    data class TestResults(
        val insertedCount: Int,
        val falsePositives: Int,
        val actualFpp: Double,
        val avgInsertionTimeMs: Double,
        val avgLookupTimeMs: Double
    )

    @SuppressLint("DefaultLocale")
    private fun updateTestResults(results: TestResults) {
        binding.testResultsText.text = """
            Performance Test Results:
            - Test Size: ${results.insertedCount} elements
            - False Positives: ${results.falsePositives}
            - Actual FPP: ${String.format("%.4f", results.actualFpp * 100)}%
            - Avg Insertion Time: ${String.format("%.3f", results.avgInsertionTimeMs)} ms
            - Avg Lookup Time: ${String.format("%.3f", results.avgLookupTimeMs)} ms
        """.trimIndent()
    }
}