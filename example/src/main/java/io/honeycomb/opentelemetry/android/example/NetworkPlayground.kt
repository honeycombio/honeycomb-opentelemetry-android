package io.honeycomb.opentelemetry.android.example

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.honeycomb.opentelemetry.android.example.ui.theme.HoneycombOpenTelemetryAndroidTheme
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

private val TAG = "NetworkPlayground"

private fun onSendNetworkRequest(setResponse: (str: String) -> Unit) {
    Log.w(TAG, "making network request")
    setResponse("loading...")

    val client = OkHttpClient.Builder().build()
    val request =
        Request.Builder()
            .url("http://10.0.2.2:1080/simple-api")
            .headers(Headers.headersOf("content-type", "application/json", "accept", "application/json"))
            .build()
    val callback =
        object : Callback {
            override fun onFailure(
                call: Call,
                e: IOException,
            ) {
                Log.w(TAG, "OkHttp error response: $e")
                setResponse("error: ${e.message}")
            }

            override fun onResponse(
                call: Call,
                response: Response,
            ) {
                val body = response.body?.string()
                Log.w(TAG, "OkHttp response: ${response.code}: ${response.message}, $body")
                setResponse("Network Request Succeeded")
                response.close()
            }
        }
    client.newCall(request).enqueue(callback)
}

@Composable
internal fun NetworkPlayground(modifier: Modifier = Modifier) {
    val networkRequestStatus = remember { mutableStateOf("") }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Button(modifier = modifier, onClick = { onSendNetworkRequest { res -> networkRequestStatus.value = res } }) {
            Text(
                text = "Make a Network Request",
            )
        }
        Text(
            text = networkRequestStatus.value,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NetworkPlaygroundPreview() {
    HoneycombOpenTelemetryAndroidTheme {
        NetworkPlayground()
    }
}
