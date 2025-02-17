package com.nguyenmoclam.kbloom

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.nguyenmoclam.kbloom.configuration.BloomFilterBuilder
import com.nguyenmoclam.kbloom.core.BloomFilter
import com.nguyenmoclam.kbloom.hashing.MurmurHash3
import com.nguyenmoclam.kbloom.logging.NoOpLogger
import com.nguyenmoclam.kbloom.monitoring.BloomFilterMetrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Base64

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : AppCompatActivity() {
    private lateinit var bloomFilter: BloomFilter<String>
    private lateinit var bloomFilterMetrics: BloomFilterMetrics<String>

    private lateinit var sharedPreferences: SharedPreferences

    private val PREFS_NAME = "BloomFilter"
    private val PREFS_KEY = "BloomFilterKey"
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private val saveChannel = Channel<Unit>(Channel.CONFLATED)
    private val consoleLogger = NoOpLogger

    @OptIn(FlowPreview::class)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // Initialize BloomFilter
        bloomFilter = initializeBloomFilter()
        // Initialize BloomFilterMetrics
        bloomFilterMetrics = BloomFilterMetrics(bloomFilter)

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val addButton = findViewById<Button>(R.id.addButton)
        val checkButton = findViewById<Button>(R.id.checkButton)
        val resultText = findViewById<TextView>(R.id.resultText)
        val metricsButton = findViewById<Button>(R.id.metricsButton)
        val metricsText = findViewById<TextView>(R.id.metricsText)

        // add email to blacklist
        addButton.setOnClickListener {
            val email = emailInput.text.toString()
            if (email.isNotEmpty()) {
                bloomFilter.put(email)
                bloomFilterMetrics.recordInsertion() // Record new insertion
                updateMetricsDisplay() // Update metrics display

                coroutineScope.launch {
                    saveChannel.receiveAsFlow().debounce(1000).collect {
                        withContext(Dispatchers.IO) {
                            saveBFToSP()
                        }
                    }
                }
                resultText.text = "Added $email to blacklist"
                emailInput.text.clear()
            } else {
                resultText.text = "Email cannot be empty"
            }
        }

        // In onCreate or wherever you're launching MetricsDetailActivity
        metricsButton.setOnClickListener {
            val intent = Intent(this, MetricsDetailActivity::class.java).apply {
                putExtra(MetricsDetailActivity.EXTRA_BLOOM_FILTER, bloomFilter.serialize())
            }
            startActivity(intent)
        }

        // Initial metrics display
        updateMetricsDisplay()

        // check if email is blacklisted
        checkButton.setOnClickListener {
            val email = emailInput.text.toString()
            if (email.isNotEmpty()) {
                val isBlacklisted = bloomFilter.mightContain(email)
                if (isBlacklisted) {
                    resultText.text = "$email is blacklisted"
                } else {
                    resultText.text = "$email is not blacklisted"
                }
                emailInput.text.clear()
            } else {
                resultText.text = "Email cannot be empty"
            }
        }
    }

    /**
     * Initialize the BloomFilter from SharedPreferences
     * If have serialized BloomFilter, deserialize it
     * Otherwise, create a new BloomFilter
     */
    private fun initializeBloomFilter(): BloomFilter<String> {
        val serialized = sharedPreferences.getString(PREFS_KEY, null)
        return if (serialized != null) {
            val byteArray = Base64.getDecoder().decode(serialized)
            BloomFilter.deserialize(
                byteArray = byteArray,
                hashFunction = MurmurHash3,
                toBytes = { it.toByteArray(Charsets.UTF_8) },
                logger = consoleLogger
            )
        } else {
            BloomFilterBuilder<String>()
                .expectedInsertions(1000)
                .falsePositiveProbability(0.01)
                .seed(42)
                .hashFunction(MurmurHash3)
                .toBytes { it.toByteArray(Charsets.UTF_8) }
                .logger(consoleLogger)
                .build()
        }
    }

    /**
     * Save the BloomFilter to SharedPreferences
     */
    private fun saveBFToSP() {
        val serialized = Base64.getEncoder().encodeToString(bloomFilter.serialize())
        sharedPreferences.edit().putString(PREFS_KEY, serialized).apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }


    // Save and load BloomFilter to/from file if needed
    private fun saveBFToFile() {
        val serialized = bloomFilter.serialize()
        val file = File(filesDir, "bloom_filter.dat")
        file.writeBytes(serialized)
    }

    private fun loadBFFromFile(): BloomFilter<String>? {
        val file = File(filesDir, "bloom_filter.dat")
        return if (file.exists()) {
            val serialized = file.readBytes()
            BloomFilter.deserialize(
                serialized,
                hashFunction = MurmurHash3,
                toBytes = { it.toByteArray(Charsets.UTF_8) })
        } else {
            null
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateMetricsDisplay() {
        val metricsText = findViewById<TextView>(R.id.metricsText)
        val fillRatio = bloomFilterMetrics.getFillRatio() * 100
        metricsText.text = "Filter is ${String.format("%.2f", fillRatio)}% full"
    }
}