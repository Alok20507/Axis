package com.racelink.controller.feature.connect

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.racelink.controller.core.ui.R
import kotlin.math.roundToInt
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
        onModeChange = viewModel::setMode,
        onLeftStickChange = viewModel::setLeftStick,
        onRightStickChange = viewModel::setRightStick,
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
    onModeChange: (ControllerMode) -> Unit,
    onLeftStickChange: (Float, Float) -> Unit,
    onRightStickChange: (Float, Float) -> Unit,
    onSteeringChange: (Float) -> Unit,
    onThrottleChange: (Float) -> Unit,
    onBrakeChange: (Float) -> Unit,
    onHandbrakeToggle: (Boolean) -> Unit,
    onButtonFlag: (Short, Boolean) -> Unit,
    onToggleGyro: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    Surface(
        color = Color(0xFF090A0D),
        contentColor = Color.White,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            // Header Bar: Sony DualSense Style Metallic Glass Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF13151D))
                    .border(1.dp, Color(0xFF262936), RoundedCornerShape(14.dp))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E212B)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("← Back", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Controller Mode Segmented Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0D0E13))
                            .padding(2.dp)
                    ) {
                        Button(
                            onClick = { onModeChange(ControllerMode.GAMEPAD) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.mode == ControllerMode.GAMEPAD) Color(0xFFD6FF61) else Color.Transparent,
                                contentColor = if (state.mode == ControllerMode.GAMEPAD) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("🎮 Gamepad", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onModeChange(ControllerMode.RACING) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.mode == ControllerMode.RACING) Color(0xFFD6FF61) else Color.Transparent,
                                contentColor = if (state.mode == ControllerMode.RACING) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("🏎️ Wheel", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF00E676)))
                    Text("120 Hz", color = Color(0xFFD6FF61), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { onToggleGyro(!state.useGyro) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.useGyro) Color(0xFFD6FF61) else Color(0xFF1E212B),
                            contentColor = if (state.useGyro) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(if (state.useGyro) "Gyro ON" else "Gyro OFF", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            if (state.mode == ControllerMode.GAMEPAD) {
                // Sony DualSense AAA Action Gamepad View
                DualSenseGamepadView(
                    state = state,
                    onLeftStickChange = onLeftStickChange,
                    onRightStickChange = onRightStickChange,
                    onThrottleChange = onThrottleChange,
                    onBrakeChange = onBrakeChange,
                    onButtonFlag = onButtonFlag
                )
            } else {
                // Racing Wheel View
                RacingWheelView(
                    state = state,
                    onSteeringChange = onSteeringChange,
                    onThrottleChange = onThrottleChange,
                    onBrakeChange = onBrakeChange,
                    onHandbrakeToggle = onHandbrakeToggle,
                    onButtonFlag = onButtonFlag
                )
            }
        }
    }
}

@Composable
private fun DualSenseGamepadView(
    state: ControllerUiState,
    onLeftStickChange: (Float, Float) -> Unit,
    onRightStickChange: (Float, Float) -> Unit,
    onThrottleChange: (Float) -> Unit,
    onBrakeChange: (Float) -> Unit,
    onButtonFlag: (Short, Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: LT Trigger + LB Bumper + Sony Left Stick + D-Pad
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SonyAdaptiveTrigger("LT (Aim)", state.brake, Color(0xFF00F0FF)) { onBrakeChange(it) }
                SonyBumperButton("LB", 0x0100.toShort(), onButtonFlag)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("L3 (MOVE)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E95A5))
                Spacer(Modifier.height(2.dp))
                SonyAnalogJoystick(
                    valueX = state.leftStickX,
                    valueY = state.leftStickY,
                    onValueChange = onLeftStickChange,
                    modifier = Modifier.size(135.dp)
                )
            }

            SonyDPadCluster(onButtonFlag)
        }

        // Center Column: PlayStation Touchpad Lightbar & System Buttons
        Column(
            modifier = Modifier.width(96.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFF1E2230), Color(0xFF0E1017))))
                    .border(2.dp, Color(0xFFD6FF61), CircleShape)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_axis_logo),
                    contentDescription = "Axis Hub",
                    modifier = Modifier.size(36.dp).clip(CircleShape)
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SonyPillButton("SHARE", 0x0020.toShort(), onButtonFlag)
                SonyPillButton("OPTIONS", 0x0010.toShort(), onButtonFlag)
            }
        }

        // Right Column: RT Trigger + RB Bumper + Sony Action ABXY + Right Stick
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SonyBumperButton("RB", 0x0200.toShort(), onButtonFlag)
                SonyAdaptiveTrigger("RT (Attack)", state.throttle, Color(0xFFFF3366)) { onThrottleChange(it) }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // PlayStation Neon ABXY Action Buttons
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SonyActionButton("Y", "△", Color(0xFFFFB703)) { onButtonFlag(0x8000.toShort(), it) }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SonyActionButton("X", "▢", Color(0xFF00F0FF)) { onButtonFlag(0x4000.toShort(), it) }
                        SonyActionButton("B", "◯", Color(0xFFFF2E63)) { onButtonFlag(0x2000.toShort(), it) }
                    }
                    SonyActionButton("A", "✕", Color(0xFF00E676)) { onButtonFlag(0x1000.toShort(), it) }
                }

                // Right Stick (Camera Look)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("R3 (LOOK)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E95A5))
                    Spacer(Modifier.height(2.dp))
                    SonyAnalogJoystick(
                        valueX = state.rightStickX,
                        valueY = state.rightStickY,
                        onValueChange = onRightStickChange,
                        modifier = Modifier.size(135.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SonyAnalogJoystick(
    valueX: Float,
    valueY: Float,
    onValueChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFF1C1F2B), Color(0xFF10121A))))
            .border(2.dp, Color(0xFF2B3042), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val radius = size.width / 2f
                        val dx = (offset.x - radius) / radius
                        val dy = -(offset.y - radius) / radius
                        onValueChange(dx.coerceIn(-1f, 1f), dy.coerceIn(-1f, 1f))
                    },
                    onDrag = { change, _ ->
                        val radius = size.width / 2f
                        val dx = (change.position.x - radius) / radius
                        val dy = -(change.position.y - radius) / radius
                        onValueChange(dx.coerceIn(-1f, 1f), dy.coerceIn(-1f, 1f))
                    },
                    onDragEnd = { onValueChange(0f, 0f) },
                    onDragCancel = { onValueChange(0f, 0f) }
                )
            }
    ) {
        // Subtle concentric guide rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(color = Color(0xFF262B3C), radius = size.minDimension / 2f - 6.dp.toPx(), style = Stroke(width = 1.dp.toPx()))
            drawCircle(color = Color(0xFF1F2332), radius = size.minDimension / 3.5f, style = Stroke(width = 1.dp.toPx()))
        }

        // Sony Style 3D Concave Thumbgrip Cap
        val knobOffsetX = (valueX * 36).dp
        val knobOffsetY = (-valueY * 36).dp

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset { IntOffset(knobOffsetX.toPx().roundToInt(), knobOffsetY.toPx().roundToInt()) }
                .size(52.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF2F3547), Color(0xFF1B1E29))))
                .border(2.dp, Color(0xFF434B63), CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF13151D))
                    .border(1.dp, Color(0xFFD6FF61).copy(alpha = 0.6f), CircleShape)
            )
        }
    }
}

@Composable
private fun SonyActionButton(
    letter: String,
    symbol: String,
    accentColor: Color,
    onPress: (Boolean) -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(38.dp)
            .shadow(if (isPressed) 2.dp else 4.dp, CircleShape)
            .clip(CircleShape)
            .background(
                if (isPressed) accentColor else Color(0xFF161822)
            )
            .border(2.dp, accentColor, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isPressed = true
                        onPress(true)
                    },
                    onDragEnd = {
                        isPressed = false
                        onPress(false)
                    },
                    onDragCancel = {
                        isPressed = false
                        onPress(false)
                    },
                    onDrag = { _, _ -> }
                )
            }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                text = letter,
                color = if (isPressed) Color.Black else accentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun SonyAdaptiveTrigger(
    label: String,
    value: Float,
    accentColor: Color,
    onValueChange: (Float) -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(100.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF14161F))
            .border(1.dp, Color(0xFF282C3D), RoundedCornerShape(10.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onValueChange(1f) },
                    onDragEnd = { onValueChange(0f) },
                    onDragCancel = { onValueChange(0f) },
                    onDrag = { _, _ -> }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(value.coerceIn(0f, 1f))
                .align(Alignment.CenterStart)
                .background(accentColor.copy(alpha = 0.85f))
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (value > 0.1f) Color.Black else Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SonyBumperButton(text: String, flag: Short, onButtonFlag: (Short, Boolean) -> Unit) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(42.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isPressed) Color(0xFFD6FF61) else Color(0xFF1E212B))
            .border(1.dp, Color(0xFF33384A), RoundedCornerShape(10.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isPressed = true
                        onButtonFlag(flag, true)
                    },
                    onDragEnd = {
                        isPressed = false
                        onButtonFlag(flag, false)
                    },
                    onDragCancel = {
                        isPressed = false
                        onButtonFlag(flag, false)
                    },
                    onDrag = { _, _ -> }
                )
            }
    ) {
        Text(text, fontSize = 10.sp, color = if (isPressed) Color.Black else Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SonyPillButton(text: String, flag: Short, onButtonFlag: (Short, Boolean) -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(40.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1D202C))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onButtonFlag(flag, true) },
                    onDragEnd = { onButtonFlag(flag, false) },
                    onDragCancel = { onButtonFlag(flag, false) },
                    onDrag = { _, _ -> }
                )
            }
    ) {
        Text(text, fontSize = 7.sp, color = Color.LightGray, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SonyDPadCluster(onButtonFlag: (Short, Boolean) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SonyDPadButton("▲", 0x0001.toShort(), onButtonFlag)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            SonyDPadButton("◀", 0x0004.toShort(), onButtonFlag)
            Box(Modifier.size(30.dp))
            SonyDPadButton("▶", 0x0008.toShort(), onButtonFlag)
        }
        SonyDPadButton("▼", 0x0002.toShort(), onButtonFlag)
    }
}

@Composable
private fun SonyDPadButton(arrow: String, flag: Short, onButtonFlag: (Short, Boolean) -> Unit) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isPressed) Color(0xFFD6FF61) else Color(0xFF1A1D28))
            .border(1.dp, Color(0xFF2C3144), RoundedCornerShape(6.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isPressed = true
                        onButtonFlag(flag, true)
                    },
                    onDragEnd = {
                        isPressed = false
                        onButtonFlag(flag, false)
                    },
                    onDragCancel = {
                        isPressed = false
                        onButtonFlag(flag, false)
                    },
                    onDrag = { _, _ -> }
                )
            }
    ) {
        Text(arrow, fontSize = 11.sp, color = if (isPressed) Color.Black else Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RacingWheelView(
    state: ControllerUiState,
    onSteeringChange: (Float) -> Unit,
    onThrottleChange: (Float) -> Unit,
    onBrakeChange: (Float) -> Unit,
    onHandbrakeToggle: (Boolean) -> Unit,
    onButtonFlag: (Short, Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Brake & Handbrake
        Column(
            modifier = Modifier.width(68.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("BRAKE", color = Color(0xFFFF4D4D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            PedalSlider(
                value = state.brake,
                onValueChange = onBrakeChange,
                fillColor = Color(0xFFFF4D4D),
                modifier = Modifier.weight(1f).width(48.dp)
            )
            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { onHandbrakeToggle(true) },
                            onDragEnd = { onHandbrakeToggle(false) },
                            onDragCancel = { onHandbrakeToggle(false) },
                            onDrag = { _, _ -> }
                        )
                    },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A1C1C))
            ) {
                Text("HANDBRAKE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(Modifier.width(12.dp))

        // Center Column: Steering Wheel
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                InteractiveSteeringWheel(
                    steering = state.leftStickX,
                    onSteeringChange = onSteeringChange,
                    modifier = Modifier.size(190.dp)
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_axis_logo),
                    contentDescription = "Axis Logo",
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Steering: ${(state.leftStickX * 100).toInt()}%",
                color = Color(0xFFD6FF61),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.width(12.dp))

        // Right Column: Throttle & ABXY Buttons
        Column(
            modifier = Modifier.width(120.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("THROTTLE", color = Color(0xFFD6FF61), fontSize = 10.sp, fontWeight = FontWeight.Bold)

            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PedalSlider(
                    value = state.throttle,
                    onValueChange = onThrottleChange,
                    fillColor = Color(0xFFD6FF61),
                    modifier = Modifier.width(48.dp).fillMaxHeight()
                )

                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SonyActionButton("Y", "△", Color(0xFFFFB703)) { onButtonFlag(0x8000.toShort(), it) }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SonyActionButton("X", "▢", Color(0xFF00F0FF)) { onButtonFlag(0x4000.toShort(), it) }
                        SonyActionButton("B", "◯", Color(0xFFFF2E63)) { onButtonFlag(0x2000.toShort(), it) }
                    }
                    SonyActionButton("A", "✕", Color(0xFF00E676)) { onButtonFlag(0x1000.toShort(), it) }
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
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF14161F))
            .border(1.dp, Color(0xFF262A3B), RoundedCornerShape(14.dp))
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
            drawCircle(color = Color(0xFF1C1F2B), radius = radius, center = center, style = Stroke(width = 22.dp.toPx()))
            drawArc(
                color = Color(0xFFD6FF61),
                startAngle = -100f,
                sweepAngle = 20f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 22.dp.toPx())
            )
            val path = Path().apply {
                moveTo(center.x - radius * 0.7f, center.y - radius * 0.7f)
                lineTo(center.x + radius * 0.7f, center.y + radius * 0.7f)
                moveTo(center.x + radius * 0.7f, center.y - radius * 0.7f)
                lineTo(center.x - radius * 0.7f, center.y + radius * 0.7f)
            }
            drawPath(path, color = Color(0xFF32374A), style = Stroke(width = 8.dp.toPx()))
            drawCircle(color = Color(0xFFD6FF61), radius = radius * 0.32f, center = center, style = Stroke(width = 2.dp.toPx()))
        }
    }
}
