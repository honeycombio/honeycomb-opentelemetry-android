package io.honeycomb.opentelemetry.android.example

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class SecondFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? = inflater.inflate(R.layout.fragment_second, container, false)

    companion object {
        @JvmStatic
        fun newInstance(name: String) =
            SecondFragment().apply {
                arguments =
                    Bundle().apply {
                    }
            }
    }
}
