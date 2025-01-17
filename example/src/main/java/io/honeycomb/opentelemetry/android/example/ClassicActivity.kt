package io.honeycomb.opentelemetry.android.example

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

private val TAG = "ClassicActivity"

class ClassicActivity : AppCompatActivity() {
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

        val firstFragmentButton = findViewById<Button>(R.id.first_fragment_button)
        firstFragmentButton.setOnClickListener {
            val fragment = FirstFragment.newInstance("second")
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        }

        val secondFragmentButton = findViewById<Button>(R.id.second_fragment_button)
        secondFragmentButton.setOnClickListener {
            val fragment = SecondFragment.newInstance("second")
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        }
    }
}
