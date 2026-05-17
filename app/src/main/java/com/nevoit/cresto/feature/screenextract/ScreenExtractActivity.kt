package com.nevoit.cresto.feature.screenextract

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle

@Suppress("DEPRECATION")
class ScreenExtractActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startService(Intent(this, ScreenExtractService::class.java))
        finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}
