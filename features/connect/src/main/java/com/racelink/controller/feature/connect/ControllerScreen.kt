package com.racelink.controller.feature.connect

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.racelink.controller.core.ui.R
import kotlin.math.atan2

@Composable
fun ControllerRoute(
    viewModel: ControllerViewModel,
    address: String,
    sessionKey: ByteArray,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(address) {
        viewModel.startSession(address, sessionKey)
    }
    ControllerScreen(
        state = state,
        onSteeringChange = viewModel::setSteering,
        onThrottleChange = viewModel::setThrottle,
        onBrakeChange = viewModel::setBrake,
        onHandbrakeToggle = viewModel::setHandbrake,
        onButtonFlag = viewModel::setButtonFlag,
        onToggleGyro = viewModel::toggleGyro,
        onBack = onBack
    )
}

@Composable
private fun ControllerScreen(
    state: ControllerUiState,
    onSteeringChange: (Float) -> Unit,
    onThrottleChange: (Float) -> Unit,
    onBrakeChange: (Float) -> Unit,
    onHandbrakeToggle: (Boolean) -> Unit,
    onButtonFlag: (Short, Boolean) -> Unit,
    onToggleGyro: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    Surface(
        color = Color(0xFF090A0C),
        contentColor = Color.White,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header Bar (Safe padding from status bar / camera cutout)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2026)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("← Back", color = Color.White, fontSize = 13.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFD6FF61)))
                    Text(state.hostAddress.ifBlank { "PC Companion" }, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("120 Hz", color = Color(0xFFD6FF61), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = { onToggleGyro(!state.useGyro) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.useGyro) Color(0xFFD6FF61) else Color(0xFF1E2026),
                        contentColor = if (state.useGyro) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (state.useGyro) "Gyro ON" else "Gyro OFF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Main Controller Workspace Grid
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Controls Column: Brake & Handbrake
                Column(
                    modifier = Modifier
                        .width(72.dp)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("BRAKE", color = Color(0xFFFF4D4D), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    PedalSlider(
                        value = state.brake,
                        onValueChange = onBrakeChange,
                        fillColor = Color(0xFFFF4D4D),
                        modifier = Modifier
                            .weight(1f)
                            .width(52.dp)
                    )
                    Button(
                        onClick = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { onHandbrakeToggle(true) },
                                    onDragEnd = { onHandbrakeToggle(false) },
                                    onDragCancel = { onHandbrakeToggle(false) },
                                    onDrag = { _, _ -> }
                                )
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A1C1C))
                    ) {
                        Text("HAND\nBRAKE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Center Column: Steering Wheel & Angle Readout
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        InteractiveSteeringWheel(
                            steering = state.steering,
                            onSteeringChange = onSteeringChange,
                            modifier = Modifier.size(200.dp)
                        )
                        // Axis Brand Icon in Hub
                        Image(
                            painter = painterResource(id = R.drawable.ic_axis_logo),
                            contentDescription = "Axis Logo Hub",
                            modifier = Modifier.size(44.dp).clip(CircleShape)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Steering: ${(state.steering * 100).toInt()}%",
                        color = Color(0xFFD6FF61),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Right Controls Column: Throttle & ABXY Buttons
                Column(
                    modifier = Modifier
                        .width(120.dp)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("THROTTLE", color = Color(0xFFD6FF61), fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PedalSlider(
                            value = state.throttle,
                            onValueChange = onThrottleChange,
                            fillColor = Color(0xFFD6FF61),
                            modifier = Modifier
                                .width(52.dp)
                                .fillMaxHeight()
                        )

                        // ABXY Diamond Button Cluster
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceEvenly,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ControllerButton("Y", Color(0xFFFFD166)) { onButtonFlag(0x8000.toShort(), it) }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                ControllerButton("X", Color(0xFF118AB2)) { onButtonFlag(0x4000.toShort(), it) }
                                ControllerButton("B", Color(0xFFEF476F)) { onButtonFlag(0x2000.toShort(), it) }
                            }
                            ControllerButton("A", Color(0xFF06D6A0)) { onButtonFlag(0x1000.toShort(), it) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PedalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    fillColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF16181F))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val pct = 1f - (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                        onValueChange(pct)
                    },
                    onDrag = { change, _ ->
                        val pct = 1f - (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                        onValueChange(pct)
                    },
                    onDragEnd = { onValueChange(0f) },
                    onDragCancel = { onValueChange(0f) }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(value.coerceIn(0f, 1f))
                .align(Alignment.BottomCenter)
                .background(fillColor.copy(alpha = 0.85f))
        )
    }
}

@Composable
private fun InteractiveSteeringWheel(
    steering: Float,
    onSteeringChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var touchAngle by remember { mutableFloatStateOf(0f) }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        touchAngle = atan2(offset.y - center.y, offset.x - center.x)
                    },
                    onDrag = { change, _ ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val currentAngle = atan2(change.position.y - center.y, change.position.x - center.x)
                        var delta = (currentAngle - touchAngle) * (180f / Math.PI.toFloat())
                        if (delta > 180f) delta -= 360f
                        if (delta < -180f) delta += 360f

                        val newSteering = (steering + delta / 180f).coerceIn(-1f, 1f)
                        onSteeringChange(newSteering)
                        touchAngle = currentAngle
                    },
                    onDragEnd = { onSteeringChange(0f) },
                    onDragCancel = { onSteeringChange(0f) }
                )
            }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 12.dp.toPx()
        val rotationAngle = steering * 180f

        rotate(rotationAngle, pivot = center) {
            // Outer Carbon Rim
            drawCircle(
                color = Color(0xFF1E2027),
                radius = radius,
                center = center,
                style = Stroke(width = 24.dp.toPx())
            )

            // Top Neon Center Marker
            drawArc(
                color = Color(0xFFD6FF61),
                startAngle = -100f,
                sweepAngle = 20f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 24.dp.toPx())
            )

            // Inner Metallic Spokes (X-Shape)
            val path = Path().apply {
                moveTo(center.x - radius * 0.7f, center.y - radius * 0.7f)
                lineTo(center.x + radius * 0.7f, center.y + radius * 0.7f)
                moveTo(center.x + radius * 0.7f, center.y - radius * 0.7f)
                lineTo(center.x - radius * 0.7f, center.y + radius * 0.7f)
            }
            drawPath(path, color = Color(0xFF353842), style = Stroke(width = 10.dp.toPx()))

            // Center Axis Ring
            drawCircle(color = Color(0xFFD6FF61), radius = radius * 0.32f, center = center, style = Stroke(width = 2.dp.toPx()))
        }
    }
}

@Composable
private fun ControllerButton(
    text: String,
    color: Color,
    onPress: (Boolean) -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.9f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onPress(true) },
                    onDragEnd = { onPress(false) },
                    onDragCancel = { onPress(false) },
                    onDrag = { _, _ -> }
                )
            }
    ) {
        Text(text, color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
