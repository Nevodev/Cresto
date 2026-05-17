package com.nevoit.cresto.feature.screenextract

import android.app.Activity
import android.content.Intent
import android.os.Bundle

@Suppress("DEPRECATION")
class ScreenExtractActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startService(Intent(this, ScreenExtractService::class.java))
        finish()
        overridePendingTransition(0, 0)
    }
}
