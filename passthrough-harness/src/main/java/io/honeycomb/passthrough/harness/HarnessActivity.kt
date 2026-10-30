package io.honeycomb.passthrough.harness

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

class HarnessActivity : FragmentActivity() {
    lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this)
        button =
            Button(this).apply {
                id = R.id.harness_button
                text = "Harness Button"
                setOnClickListener { }
            }
        root.addView(button, FrameLayout.LayoutParams(300, 80))
        // A clickable view with no resource id (View.NO_ID), as programmatically built UIs have.
        val noId =
            Button(this).apply {
                text = "No Id Button"
                setOnClickListener { }
            }
        root.addView(noId, FrameLayout.LayoutParams(300, 80).apply { topMargin = 100 })
        // A clickable view whose id was generated at runtime, so it has no resource entry name.
        val generated =
            Button(this).apply {
                id = View.generateViewId()
                text = "Generated Id Button"
                setOnClickListener { }
            }
        root.addView(generated, FrameLayout.LayoutParams(300, 80).apply { topMargin = 200 })
        setContentView(root, ViewGroup.LayoutParams(1080, 1920))
    }
}

class HarnessFragment : Fragment()
