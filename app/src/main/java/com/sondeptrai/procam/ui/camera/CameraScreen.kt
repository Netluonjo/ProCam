package com.sondeptrai.procam.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.sondeptrai.procam.core.camera.*
import com.sondeptrai.procam.core.gl.CameraGlView
import com.sondeptrai.procam.ui.theme.DslrBlack
import com.sondeptrai.procam.ui.theme.ProCamRed
import com.sondeptrai.procam.ui.viewmodel.CameraViewModel

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val levelState by viewModel.levelState.collectAsState()
    val isSteady by viewModel.isSteady.collectAsState()
    val histogramData by viewModel.histogramData.collectAsState()
    val context = LocalContext.current

    var glViewRef by remember { mutableStateOf<CameraGlView?>(null) }

    // Synchronize Shader state with CameraGlView
    LaunchedEffect(uiState.isFocusPeakingEnabled, uiState.peakingColor) {
        glViewRef?.updatePeaking(uiState.isFocusPeakingEnabled, uiState.peakingColor)
    }
    LaunchedEffect(uiState.isZebraEnabled, uiState.zebraThreshold) {
        glViewRef?.updateZebra(uiState.isZebraEnabled, uiState.zebraThreshold)
    }
    LaunchedEffect(uiState.isFrontCamera) {
        glViewRef?.updateFrontCamera(uiState.isFrontCamera)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Black
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
        ) {
            // 1. ProCam Top Control Bar (Status Bar + Sub-header Strip)
            CameraTopBar(
                state = uiState,
                onFlashToggle = { viewModel.onFlashToggle() },
                onRawToggle = { viewModel.onRawToggle() },
                onTiffToggle = { viewModel.onTiffToggle() },
                onSbrtToggle = { viewModel.onSbrtToggle() },
                onAebToggle = { viewModel.onAebToggle() },
                onSetToggle = { viewModel.onSetMenuToggle() },
                onFlipCamera = { viewModel.onSwitchCamera() }
            )

            // 2. Viewfinder Section with GL Surface and ProCam Overlays (True 16:9 / aspect ratio)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                val targetRatio = uiState.aspectRatio.portraitRatio
                val containerRatio = maxWidth / maxHeight

                val (viewfinderWidth, viewfinderHeight) = if (containerRatio > targetRatio) {
                    (maxHeight * targetRatio) to maxHeight
                } else {
                    maxWidth to (maxWidth / targetRatio)
                }

                Box(
                    modifier = Modifier
                        .size(viewfinderWidth, viewfinderHeight)
                        .clipToBounds()
                        .pointerInput(uiState.hardwareInfo.zoomRatioRange, uiState.activeManualParam, uiState.currentIso, uiState.isAutoIso, uiState.currentEvIndex, uiState.hardwareInfo.aeCompensationRange) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                if (zoom != 1f) {
                                    val newZoom = (uiState.currentZoomRatio * zoom).coerceIn(
                                        uiState.hardwareInfo.zoomRatioRange.lower,
                                        uiState.hardwareInfo.zoomRatioRange.upper
                                    )
                                    viewModel.onZoomChanged(newZoom)
                                } else if (uiState.activeManualParam == ManualParameter.ISO && kotlin.math.abs(pan.x) > 20f) {
                                    // Halide signature swipe-to-adjust ISO gesture (horizontal swipe)
                                    val currentIndex = if (uiState.isAutoIso) 0 else {
                                        val idx = HALIDE_ISO_VALUES.indexOfFirst { it.second == uiState.currentIso }
                                        if (idx != -1) idx else 4
                                    }
                                    val delta = if (pan.x > 0) 1 else -1
                                    val nextIdx = (currentIndex + delta).coerceIn(0, HALIDE_ISO_VALUES.size - 1)
                                    val (_, targetIso) = HALIDE_ISO_VALUES[nextIdx]
                                    viewModel.onIsoChanged(if (targetIso == -1) 100 else targetIso, targetIso == -1)
                                } else if (kotlin.math.abs(pan.y) > 12f) {
                                    // Halide signature vertical swipe-to-adjust EV (Exposure Value)
                                    val stepRange = uiState.hardwareInfo.aeCompensationRange
                                    val delta = if (pan.y < 0) 1 else -1 // Dragging up increases EV (+), dragging down decreases EV (-)
                                    val nextEv = (uiState.currentEvIndex + delta).coerceIn(stepRange.lower, stepRange.upper)
                                    if (nextEv != uiState.currentEvIndex) {
                                        viewModel.onEvChanged(nextEv)
                                    }
                                }
                            }
                        }
                ) {
                // Real-time OpenGL ES Preview with Shader filters & Mirroring
                AndroidView(
                    factory = { ctx ->
                        CameraGlView(ctx) { surface ->
                            viewModel.onSurfaceCreated(surface)
                        }.also {
                            glViewRef = it
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )


                // Grid & Focus Target Overlay
                ViewfinderOverlay(
                    gridType = uiState.gridType,
                    onFocusPointSelected = { /* Tap-to-focus point */ }
                )

                // Tiltmeter / Artificial Horizon Level (Center of Viewfinder)
                if (uiState.isTiltMeterEnabled) {
                    TiltMeterWidget(
                        levelOrientation = levelState,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Stereo Audio VU Meter (Left Edge in Video Mode)
                if (uiState.selectedMode == ShootingMode.VIDEO) {
                    AudioVuMeterWidget(
                        decibelLevel = uiState.audioDecibelLevel,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 12.dp)
                    )

                    // Video Recording Indicator Badge (Bottom Left)
                    if (uiState.isRecordingVideo) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 14.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(ProCamRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val minutes = uiState.videoRecordingSeconds / 60
                            val seconds = uiState.videoRecordingSeconds % 60
                            Text(
                                text = String.format("REC %02d:%02d", minutes, seconds),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Live Luminance Histogram Overlay (Iconic Bottom-Right placement in ProCam)
                if (uiState.isHistogramEnabled) {
                    HistogramWidget(
                        histogramData = histogramData,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 12.dp)
                    )
                }

                // ProCam Quick Zoom Preset Widget (1x, 2x, 3x, 5x)
                ZoomControlWidget(
                    currentZoom = uiState.currentZoomRatio,
                    maxZoom = uiState.hardwareInfo.zoomRatioRange.upper,
                    isFrontCamera = uiState.isFrontCamera,
                    onZoomSelected = { targetZoom -> viewModel.onZoomChanged(targetZoom) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                )

                // Shutter Flash White Feedback
                if (uiState.isShutterFlashing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                    )
                }
            }
            }

            // 3. ProCam 6-Column Parameter Strip [ AE ] [ SEC ] [ ISO ] [ AF ] [ AWB ] [ E/F ]
            ManualParamSelectorBar(
                state = uiState,
                onParamSelected = { param -> viewModel.onParamSelected(param) }
            )

            // 4. Manual Control Dial: Halide 3D Rotary Drum Dial for EV (+3 exposure) or ISO, or ProCam Ruler for other parameters
            if (uiState.activeManualParam == ManualParameter.ISO || uiState.activeManualParam == ManualParameter.AE) {
                HalideIsoDial(
                    state = uiState,
                    onIsoChanged = { iso, isAuto -> viewModel.onIsoChanged(iso, isAuto) },
                    onEvChanged = { ev -> viewModel.onEvChanged(ev) },
                    initialModeIsEv = (uiState.activeManualParam == ManualParameter.ISO)
                )
            } else {
                ProCamRulerDial(
                    state = uiState,
                    onIsoChanged = { iso, isAuto -> viewModel.onIsoChanged(iso, isAuto) },
                    onShutterChanged = { nanos, isAuto -> viewModel.onShutterChanged(nanos, isAuto) },
                    onFocusChanged = { dist, isAuto -> viewModel.onFocusChanged(dist, isAuto) },
                    onKelvinChanged = { k, isAuto -> viewModel.onKelvinChanged(k, isAuto) },
                    onEvChanged = { ev -> viewModel.onEvChanged(ev) },
                    onToggleAeAfLock = { viewModel.onAeAfLockToggle() },
                    onToggleWbLock = { viewModel.onWbLockToggle() }
                )
            }

            // 5. Expandable Shooting Mode Selector Drawer (Photo, Video, Burst, Slow Shutter, Portrait)
            ModeSelector(
                selectedMode = uiState.selectedMode,
                isExpanded = uiState.isModeDrawerExpanded,
                onModeSelected = { mode ->
                    viewModel.onModeSelected(mode)
                    viewModel.onModeDrawerToggle() // Collapse drawer after selection
                }
            )

            // 6. Iconic ProCam Bottom Shutter & Controls Row
            ShutterControlSection(
                mode = uiState.selectedMode,
                isRecording = uiState.isRecordingVideo,
                isFrontCamera = uiState.isFrontCamera,
                timerSeconds = uiState.timerSeconds,
                currentZoomRatio = uiState.currentZoomRatio,
                isDrawerExpanded = uiState.isModeDrawerExpanded,
                lastCapturedUri = uiState.lastCapturedUri,
                onShutterClick = { viewModel.onShutterPressed() },
                onGalleryClick = { viewModel.onOpenGallery(context) },
                onFlipCameraClick = { viewModel.onSwitchCamera() },
                onZoomCycle = {
                    val current = uiState.currentZoomRatio
                    val max = uiState.hardwareInfo.zoomRatioRange.upper
                    val next = when {
                        current < 1.9f -> 2.0f
                        current < 2.9f -> 3.0f
                        current < 4.9f && max >= 5.0f -> 5.0f
                        else -> 1.0f
                    }
                    viewModel.onZoomChanged(next)
                },
                onModeDrawerToggle = { viewModel.onModeDrawerToggle() },
                onTimerCycle = { viewModel.onTimerCycle() }
            )
        }

        // 7. ProCam Settings Modal Dialog (When tapping SET)
        if (uiState.isSetMenuOpen) {
            ProCamSettingsModal(
                state = uiState,
                onDismiss = { viewModel.onSetMenuToggle() },
                onPeakingToggle = { viewModel.onPeakingToggle() },
                onZebraToggle = { viewModel.onZebraToggle() },
                onGridToggle = { viewModel.onGridToggle() },
                onTiltMeterToggle = { viewModel.onTiltMeterToggle() },
                onHistogramToggle = { viewModel.onHistogramToggle() },
                onAspectRatioToggle = { viewModel.onAspectRatioToggle() }
            )
        }
    }
}
