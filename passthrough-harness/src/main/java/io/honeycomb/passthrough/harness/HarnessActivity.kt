package io.honeycomb.passthrough.harness

import android.os.Bundle
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
        root.addView(button, FrameLayout.LayoutParams(400, 200))
        setContentView(root, ViewGroup.LayoutParams(1080, 1920))
    }
}

class HarnessFragment : Fragment()
