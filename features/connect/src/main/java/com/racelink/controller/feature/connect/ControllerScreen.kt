package com.racelink.controller.feature.connect

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.BatteryManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.racelink.controller.core.haptics.HapticsManager
import com.racelink.controller.core.storage.ControlElementTransform
import com.racelink.controller.core.storage.ControllerPreferencesStore
import com.racelink.controller.core.ui.R
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt

@Composable
fun ControllerRoute(
    viewModel: ControllerViewModel,
    address: String,
    sessionKey: ByteArray,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    // Auto-enforce Landscape Orientation when Controller Screen opens
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(address) {
        if (address != "preview") {
            viewModel.startSession(address, sessionKey)
        }
    }

    ControllerScreen(
        state = state,
        isPreviewMode = address == "preview",
        onModeChange = viewModel::setMode,
        onLeftStickChange = viewModel::setLeftStick,
        onRightStickChange = viewModel::setRightStick,
        onSteeringChange = viewModel::setSteering,
        onThrottleChange = viewModel::setThrottle,
        onBrakeChange = viewModel::setBrake,
        onHandbrakeToggle = viewModel::setHandbrake,
        onButtonFlag = viewModel::setButtonFlag,
        onToggleGyro = viewModel::toggleGyro,
        onRecalibrateGyro = viewModel::recalibrateGyro,
        onBack = onBack
    )
}

@Composable
private fun ControllerScreen(
    state: ControllerUiState,
    isPreviewMode: Boolean,
    onModeChange: (ControllerMode) -> Unit,
    onLeftStickChange: (Float, Float) -> Unit,
    onRightStickChange: (Float, Float) -> Unit,
    onSteeringChange: (Float) -> Unit,
    onThrottleChange: (Float) -> Unit,
    onBrakeChange: (Float) -> Unit,
    onHandbrakeToggle: (Boolean) -> Unit,
    onButtonFlag: (Short, Boolean) -> Unit,
    onToggleGyro: (Boolean) -> Unit,
    onRecalibrateGyro: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember { ControllerPreferencesStore(context) }
    var isEditMode by remember { mutableStateOf(isPreviewMode) }

    val batteryPct = remember(context) {
        runCatching {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
        }.getOrDefault(100)
    }

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
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
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
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("← Back", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Mode Toggle: Gamepad vs Racing Wheel
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
                            modifier = Modifier.height(26.dp)
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
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text("🏎️ Wheel", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text("Profile: ${store.currentProfile}", color = Color(0xFFD6FF61), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Recalibrate Gyro Center Neutral Point Button
                    Button(
                        onClick = onRecalibrateGyro,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E212B)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("🎯 Recalibrate Center", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00F0FF))
                    }

                    // Edit Layout Toggle Button
                    Button(
                        onClick = { isEditMode = !isEditMode },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEditMode) Color(0xFFFFB703) else Color(0xFF1E212B),
                            contentColor = if (isEditMode) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(if (isEditMode) "💾 Save Layout" else "✏️ Edit Layout", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onToggleGyro(!state.useGyro) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.useGyro) Color(0xFFD6FF61) else Color(0xFF1E212B),
                            contentColor = if (state.useGyro) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(if (state.useGyro) "Gyro ON" else "Gyro OFF", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            if (state.mode == ControllerMode.GAMEPAD) {
                DualSenseGamepadView(
                    state = state,
                    store = store,
                    isEditMode = isEditMode,
                    onLeftStickChange = onLeftStickChange,
                    onRightStickChange = onRightStickChange,
                    onThrottleChange = onThrottleChange,
                    onBrakeChange = onBrakeChange,
                    onButtonFlag = onButtonFlag
                )
            } else {
                RacingWheelView(
                    state = state,
                    store = store,
                    isEditMode = isEditMode,
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
    store: ControllerPreferencesStore,
    isEditMode: Boolean,
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
        // Left Column: LT + LB + Left Stick + D-Pad
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            DraggableControlContainer("left_triggers", store, store.currentProfile, "GAMEPAD", isEditMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SonyAdaptiveTrigger("LT (Aim)", state.brake, Color(0xFF00F0FF)) { onBrakeChange(it) }
                    SonyBumperButton("LB", 0x0100.toShort(), onButtonFlag)
                }
            }

            DraggableControlContainer("left_stick", store, store.currentProfile, "GAMEPAD", isEditMode) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("L3 (MOVE)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E95A5))
                    Spacer(Modifier.height(2.dp))
                    SonyAnalogJoystick(
                        valueX = state.leftStickX,
                        valueY = state.leftStickY,
                        onValueChange = onLeftStickChange,
                        modifier = Modifier.size(130.dp)
                    )
                }
            }

            DraggableControlContainer("dpad", store, store.currentProfile, "GAMEPAD", isEditMode) {
                SonyDPadCluster(onButtonFlag)
            }
        }

        // Center Column: Touchpad Hub & System Buttons
        Column(
            modifier = Modifier.width(96.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFF1E2230), Color(0xFF0E1017))))
                    .border(2.dp, Color(0xFFD6FF61), CircleShape)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_axis_logo),
                    contentDescription = "Axis Hub",
                    modifier = Modifier.size(34.dp).clip(CircleShape)
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SonyPillButton("SELECT", 0x0020.toShort(), onButtonFlag)
                SonyPillButton("START", 0x0010.toShort(), onButtonFlag)
            }
        }

        // Right Column: RT + RB + ABXY + Right Stick
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            DraggableControlContainer("right_triggers", store, store.currentProfile, "GAMEPAD", isEditMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SonyBumperButton("RB", 0x0200.toShort(), onButtonFlag)
                    SonyAdaptiveTrigger("RT (Attack)", state.throttle, Color(0xFFFF3366)) { onThrottleChange(it) }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DraggableControlContainer("action_buttons", store, store.currentProfile, "GAMEPAD", isEditMode) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SonyActionButton("Y", Color(0xFFFFB703)) { onButtonFlag(0x8000.toShort(), it) }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SonyActionButton("X", Color(0xFF00F0FF)) { onButtonFlag(0x4000.toShort(), it) }
                            SonyActionButton("B", Color(0xFFFF2E63)) { onButtonFlag(0x2000.toShort(), it) }
                        }
                        SonyActionButton("A", Color(0xFF00E676)) { onButtonFlag(0x1000.toShort(), it) }
                    }
                }

                DraggableControlContainer("right_stick", store, store.currentProfile, "GAMEPAD", isEditMode) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("R3 (LOOK)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E95A5))
                        Spacer(Modifier.height(2.dp))
                        SonyAnalogJoystick(
                            valueX = state.rightStickX,
                            valueY = state.rightStickY,
                            onValueChange = onRightStickChange,
                            modifier = Modifier.size(130.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RacingWheelView(
    state: ControllerUiState,
    store: ControllerPreferencesStore,
    isEditMode: Boolean,
    onSteeringChange: (Float) -> Unit,
    onThrottleChange: (Float) -> Unit,
    onBrakeChange: (Float) -> Unit,
    onHandbrakeToggle: (Boolean) -> Unit,
    onButtonFlag: (Short, Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        // 1. Separate Draggable Brake Pedal
        DraggableControlContainer("brake_pedal", store, store.currentProfile, "RACING", isEditMode) {
            Column(
                modifier = Modifier.width(64.dp).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("BRAKE", color = Color(0xFFFF4D4D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                PedalSlider(
                    value = state.brake,
                    onValueChange = onBrakeChange,
                    fillColor = Color(0xFFFF4D4D),
                    modifier = Modifier.fillMaxHeight(0.85f).width(48.dp)
                )
            }
        }

        // 2. Separate Draggable Handbrake Button
        DraggableControlContainer("handbrake_button", store, store.currentProfile, "RACING", isEditMode) {
            Button(
                onClick = {},
                modifier = Modifier
                    .width(72.dp)
                    .height(54.dp)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            onHandbrakeToggle(true)
                            val up = waitForUpOrCancellation()
                            onHandbrakeToggle(false)
                        }
                    },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9E1B1B))
            ) {
                Text("HANDBRAKE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // 3. Separate Draggable 360° Steering Wheel with Rotation Haptics
        DraggableControlContainer("wheel", store, store.currentProfile, "RACING", isEditMode, modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(contentAlignment = Alignment.Center) {
                    InteractiveSteeringWheel(
                        steering = state.leftStickX,
                        onSteeringChange = onSteeringChange,
                        modifier = Modifier.size(195.dp)
                    )
                    Image(
                        painter = painterResource(id = R.drawable.ic_axis_logo),
                        contentDescription = "Axis Logo",
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Steering: ${(state.leftStickX * 100).toInt()}%",
                    color = Color(0xFFD6FF61),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 4. Separate Draggable Throttle Pedal
        DraggableControlContainer("throttle_pedal", store, store.currentProfile, "RACING", isEditMode) {
            Column(
                modifier = Modifier.width(64.dp).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("THROTTLE", color = Color(0xFFD6FF61), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                PedalSlider(
                    value = state.throttle,
                    onValueChange = onThrottleChange,
                    fillColor = Color(0xFFD6FF61),
                    modifier = Modifier.fillMaxHeight(0.85f).width(48.dp)
                )
            }
        }

        // 5. Separate Draggable Action Buttons Cluster (ABXY)
        DraggableControlContainer("wheel_action_buttons", store, store.currentProfile, "RACING", isEditMode) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SonyActionButton("Y", Color(0xFFFFB703)) { onButtonFlag(0x8000.toShort(), it) }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SonyActionButton("X", Color(0xFF00F0FF)) { onButtonFlag(0x4000.toShort(), it) }
                    SonyActionButton("B", Color(0xFFFF2E63)) { onButtonFlag(0x2000.toShort(), it) }
                }
                SonyActionButton("A", Color(0xFF00E676)) { onButtonFlag(0x1000.toShort(), it) }
            }
        }
    }
}

@Composable
private fun DraggableControlContainer(
    elementKey: String,
    store: ControllerPreferencesStore,
    activeProfile: String,
    activeMode: String,
    isEditMode: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var transform by remember(activeProfile, activeMode, elementKey) {
        mutableStateOf(store.getTransform(activeProfile, activeMode, elementKey))
    }

    var dragOffset by remember(activeProfile, activeMode, elementKey) {
        mutableStateOf(Offset(transform.offsetX, transform.offsetY))
    }
    var currentScale by remember(activeProfile, activeMode, elementKey) {
        mutableFloatStateOf(transform.scale)
    }

    Box(
        modifier = modifier
            .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
            .graphicsLayer {
                scaleX = currentScale
                scaleY = currentScale
            }
            .then(
                if (isEditMode) {
                    Modifier
                        .border(1.5.dp, Color(0xFFD6FF61), RoundedCornerShape(12.dp))
                        .background(Color(0xFFD6FF61).copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .pointerInput(activeProfile, activeMode, elementKey) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val newX = dragOffset.x + dragAmount.x
                                val newY = dragOffset.y + dragAmount.y
                                dragOffset = Offset(newX, newY)
                                val updated = ControlElementTransform(newX, newY, currentScale)
                                store.setTransform(activeProfile, activeMode, elementKey, updated)
                            }
                        }
                } else Modifier
            )
    ) {
        content()

        if (isEditMode) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = (-12).dp, x = 12.dp)
                    .background(Color.Black, CircleShape)
                    .border(1.dp, Color(0xFFD6FF61), CircleShape)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "-",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable {
                        val newScale = (currentScale - 0.1f).coerceIn(0.6f, 2.0f)
                        currentScale = newScale
                        store.setTransform(activeProfile, activeMode, elementKey, ControlElementTransform(dragOffset.x, dragOffset.y, newScale))
                    }
                )
                Text(
                    "${(currentScale * 100).toInt()}%",
                    color = Color(0xFFD6FF61),
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
                Text(
                    "+",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable {
                        val newScale = (currentScale + 0.1f).coerceIn(0.6f, 2.0f)
                        currentScale = newScale
                        store.setTransform(activeProfile, activeMode, elementKey, ControlElementTransform(dragOffset.x, dragOffset.y, newScale))
                    }
                )
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
    val context = LocalContext.current
    val haptics = remember(context) { HapticsManager(context) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFF1C1F2B), Color(0xFF10121A))))
            .border(2.dp, Color(0xFF2B3042), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        haptics.tick()
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
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = Color(0xFF262B3C), radius = size.minDimension / 2f - 6.dp.toPx(), style = Stroke(width = 1.dp.toPx()))
            drawCircle(color = Color(0xFF1F2332), radius = size.minDimension / 3.5f, style = Stroke(width = 1.dp.toPx()))
        }

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
    accentColor: Color,
    onPress: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val haptics = remember(context) { HapticsManager(context) }
    var isPressed by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(38.dp)
            .shadow(if (isPressed) 2.dp else 4.dp, CircleShape)
            .clip(CircleShape)
            .background(if (isPressed) accentColor else Color(0xFF161822))
            .border(2.dp, accentColor, CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isPressed = true
                    haptics.tick()
                    onPress(true)
                    val up = waitForUpOrCancellation()
                    isPressed = false
                    onPress(false)
                }
            }
    ) {
        Text(
            text = letter,
            color = if (isPressed) Color.Black else accentColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun SonyAdaptiveTrigger(
    label: String,
    value: Float,
    accentColor: Color,
    onValueChange: (Float) -> Unit,
) {
    val context = LocalContext.current
    val haptics = remember(context) { HapticsManager(context) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(100.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF14161F))
            .border(1.dp, Color(0xFF282C3D), RoundedCornerShape(10.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    haptics.tick()
                    onValueChange(1f)
                    val up = waitForUpOrCancellation()
                    onValueChange(0f)
                }
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
    val context = LocalContext.current
    val haptics = remember(context) { HapticsManager(context) }
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
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isPressed = true
                    haptics.tick()
                    onButtonFlag(flag, true)
                    val up = waitForUpOrCancellation()
                    isPressed = false
                    onButtonFlag(flag, false)
                }
            }
    ) {
        Text(text, fontSize = 10.sp, color = if (isPressed) Color.Black else Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SonyPillButton(text: String, flag: Short, onButtonFlag: (Short, Boolean) -> Unit) {
    val context = LocalContext.current
    val haptics = remember(context) { HapticsManager(context) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(42.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1D202C))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    haptics.tick()
                    onButtonFlag(flag, true)
                    val up = waitForUpOrCancellation()
                    onButtonFlag(flag, false)
                }
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
    val context = LocalContext.current
    val haptics = remember(context) { HapticsManager(context) }
    var isPressed by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isPressed) Color(0xFFD6FF61) else Color(0xFF1A1D28))
            .border(1.dp, Color(0xFF2C3144), RoundedCornerShape(6.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isPressed = true
                    haptics.tick()
                    onButtonFlag(flag, true)
                    val up = waitForUpOrCancellation()
                    isPressed = false
                    onButtonFlag(flag, false)
                }
            }
    ) {
        Text(arrow, fontSize = 11.sp, color = if (isPressed) Color.Black else Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PedalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    fillColor: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = remember(context) { HapticsManager(context) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF14161F))
            .border(1.dp, Color(0xFF262A3B), RoundedCornerShape(14.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        haptics.tick()
                        val pct = 1f - (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                        onValueChange(pct)
                    },
                    onDrag = { change, _ ->
                        haptics.rumble(25, 170)
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
    val context = LocalContext.current
    val haptics = remember(context) { HapticsManager(context) }

    var accumulatedSteering by remember { mutableFloatStateOf(0f) }
    var startTouchAngle by remember { mutableFloatStateOf(0f) }
    var lastHapticSteering by remember { mutableFloatStateOf(0f) }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        haptics.tick()
                        val center = Offset(size.width / 2f, size.height / 2f)
                        startTouchAngle = atan2(offset.y - center.y, offset.x - center.x)
                    },
                    onDrag = { change, _ ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val currentTouchAngle = atan2(change.position.y - center.y, change.position.x - center.x)
                        var deltaRad = currentTouchAngle - startTouchAngle

                        // Normalize radian wrapping [-PI, PI]
                        if (deltaRad > Math.PI) deltaRad -= (2 * Math.PI).toFloat()
                        if (deltaRad < -Math.PI) deltaRad += (2 * Math.PI).toFloat()

                        val deltaNormalized = deltaRad / (Math.PI.toFloat() * 0.5f) // 90 deg = 100% lock
                        val targetSteering = (accumulatedSteering + deltaNormalized).coerceIn(-1f, 1f)

                        // Tactile haptic feedback on wheel rotation ticks
                        if (abs(targetSteering - lastHapticSteering) > 0.12f) {
                            haptics.tick()
                            lastHapticSteering = targetSteering
                        }

                        // Max Steering Lock Crash Rumble
                        if (abs(targetSteering) >= 0.98f && abs(accumulatedSteering) < 0.98f) {
                            haptics.rumble(80, 255)
                        }

                        accumulatedSteering = targetSteering
                        onSteeringChange(targetSteering)
                        startTouchAngle = currentTouchAngle
                    },
                    onDragEnd = {
                        accumulatedSteering = 0f
                        onSteeringChange(0f)
                    },
                    onDragCancel = {
                        accumulatedSteering = 0f
                        onSteeringChange(0f)
                    }
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
