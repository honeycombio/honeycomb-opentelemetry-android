package io.honeycomb.opentelemetry.android.example

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Button

private val TAG = "ClassicActivity"

class ClassicActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classic)

        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
            Log.i(TAG, "Clicked!")
        }
    }
}
