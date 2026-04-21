package com.protelion.hexserver

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.view.Gravity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val textView = TextView(this).apply {
            text = "HEX Service Application\nRunning in background"
            gravity = Gravity.CENTER
            textSize = 20f
        }
        setContentView(textView)
    }
}
