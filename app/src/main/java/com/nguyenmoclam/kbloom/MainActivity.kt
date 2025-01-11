package com.nguyenmoclam.kbloom

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val bf = BloomFilter.create(
            expectedInsertions = 1000,
            fpp = 0.01,
            seed = 42
        )

        // Insert some elements
        bf.put("apple")
        bf.put("banana")

        // Check if the BloomFilter might contain the element
        val checkApple = bf.mightContain("apple")
        println("Contain Apple  $checkApple")
    }
}