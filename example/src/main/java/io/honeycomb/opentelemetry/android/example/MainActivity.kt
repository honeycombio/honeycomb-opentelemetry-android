package io.honeycomb.opentelemetry.android.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.honeycomb.opentelemetry.android.example.ui.theme.HoneycombOpenTelemetryAndroidTheme
import io.opentelemetry.android.OpenTelemetryRum

private val TAG = "MainActivity"

// The tabs in the sample app.
enum class PlaygroundTab(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    CORE("Core", Icons.Outlined.Home, Icons.Filled.Home),
    UI("UI", Icons.Outlined.Palette, Icons.Filled.Palette),
    NETWORK("Network", Icons.Outlined.Language, Icons.Filled.Language),
}

/**
 * An activity with various UI elements that cause telemetry to be emitted.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as ExampleApp
        val otelRum = app.otelRum

        enableEdgeToEdge()
        setContent {
            val currentTab = remember { mutableStateOf(PlaygroundTab.CORE) }

            HoneycombOpenTelemetryAndroidTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = { NavBar(currentTab) },
                ) { innerPadding ->
                    Playground(
                        otelRum,
                        currentTab,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Playground(
    otel: OpenTelemetryRum?,
    currentTab: State<PlaygroundTab>,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize().padding(20.dp),
    ) {
        Text(
            text =
                "The following components demonstrate " +
                    "auto-instrumentation features of the " +
                    "Honeycomb Android SDK.",
        )
        Spacer(modifier = Modifier.height(50.dp))

        when (currentTab.value) {
            PlaygroundTab.CORE -> {
                CorePlayground(otel)
            }
            PlaygroundTab.UI -> {
                UIPlayground()
            }
            PlaygroundTab.NETWORK -> {
                NetworkPlayground()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlaygroundPreview() {
    HoneycombOpenTelemetryAndroidTheme {
        Playground(null, remember { mutableStateOf(PlaygroundTab.CORE) })
    }
}

@Composable
private fun NavBar(currentTab: MutableState<PlaygroundTab>) {
    NavigationBar {
        PlaygroundTab.entries.forEach {
            NavigationBarItem(
                icon = { Icon(if (it == currentTab.value) it.selectedIcon else it.icon, contentDescription = "Core") },
                label = { Text(it.label) },
                selected = it == currentTab.value,
                onClick = { currentTab.value = it },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NavBarPreview() {
    HoneycombOpenTelemetryAndroidTheme {
        NavBar(remember { mutableStateOf(PlaygroundTab.CORE) })
    }
}
