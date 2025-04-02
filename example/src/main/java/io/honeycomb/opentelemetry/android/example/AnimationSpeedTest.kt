package io.honeycomb.opentelemetry.android.example

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.honeycomb.opentelemetry.android.example.ui.theme.HoneycombOpenTelemetryAndroidTheme

// This is used below, to make animation take certain lengths of time.
enum class AnimationSpeed(
    val sleepTime: Long,
) {
    NORMAL(0),
    SLOW(32),
    FROZEN(1400),
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AnimationSpeedTest() {
    // This component is specifically designed to render slowly,
    // to demonstrate slow render detections.
    val angle =
        rememberInfiniteTransition(
            label = "AngleTransition",
        ).animateFloat(
            initialValue = 0.0f,
            targetValue = 360.0f,
            animationSpec =
                infiniteRepeatable(
                    tween(1000, easing = LinearEasing),
                    RepeatMode.Restart,
                ),
            label = "AngleAnimation",
        )

    var animationSpeed by remember {
        mutableStateOf(AnimationSpeed.NORMAL)
    }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Spacer(
            modifier =
                Modifier
                    .height(100.dp)
                    .width(100.dp)
                    .drawBehind {
                        // This is what makes it slow.
                        if (animationSpeed.sleepTime > 0) {
                            Thread.sleep(animationSpeed.sleepTime)
                        }
                        drawCircle(color = Color.Gray)
                        inset(5.dp.toPx()) {
                            drawCircle(color = Color.Yellow)
                            val top = Offset(center.x, 0.0f)
                            rotate(degrees = angle.value) {
                                drawLine(
                                    color = Color.Gray,
                                    start = top,
                                    end = center,
                                    strokeWidth = 5.dp.toPx(),
                                )
                            }
                        }
                    },
        )
        Spacer(modifier = Modifier.height(20.dp))
        SingleChoiceSegmentedButtonRow {
            SegmentedButton(
                selected = animationSpeed == AnimationSpeed.NORMAL,
                onClick = { animationSpeed = AnimationSpeed.NORMAL },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
            ) {
                Text(text = "Normal")
            }
            SegmentedButton(
                selected = animationSpeed == AnimationSpeed.SLOW,
                onClick = { animationSpeed = AnimationSpeed.SLOW },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
            ) {
                Text(text = "Slow")
            }
            SegmentedButton(
                selected = animationSpeed == AnimationSpeed.FROZEN,
                onClick = { animationSpeed = AnimationSpeed.FROZEN },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
            ) {
                Text(text = "Frozen")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AnimationSpeedTestPreview() {
    HoneycombOpenTelemetryAndroidTheme {
        AnimationSpeedTest()
    }
}
