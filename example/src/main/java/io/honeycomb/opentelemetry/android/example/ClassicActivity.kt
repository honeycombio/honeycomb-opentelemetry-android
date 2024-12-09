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

        val backButton = findViewById<Button>(R.id.back_button)
        backButton.setOnClickListener {
            finish()
        }

        val exampleButton = findViewById<Button>(R.id.example_button)
        exampleButton.setOnClickListener {
            Log.i(TAG, "Clicked!")
        }
    }
}
