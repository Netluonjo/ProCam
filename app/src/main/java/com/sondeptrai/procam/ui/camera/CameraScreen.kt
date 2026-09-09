package com.sondeptrai.procam.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.sondeptrai.procam.core.gl.CameraGlView
import com.sondeptrai.procam.ui.theme.DslrBlack
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

    var glViewRef by remember { mutableStateOf<CameraGlView?>(null) }

    // Synchronize Shader state with CameraGlView
    LaunchedEffect(uiState.isFocusPeakingEnabled, uiState.peakingColor) {
        glViewRef?.updatePeaking(uiState.isFocusPeakingEnabled, uiState.peakingColor)
    }
    LaunchedEffect(uiState.isZebraEnabled, uiState.zebraThreshold) {
        glViewRef?.updateZebra(uiState.isZebraEnabled, uiState.zebraThreshold)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DslrBlack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DslrBlack)
        ) {
            // 1. ProCam Top Control Bar
            CameraTopBar(
                state = uiState,
                onFlashToggle = { viewModel.onFlashToggle() },
                onRawToggle = { viewModel.onRawToggle() },
                onAspectRatioToggle = { viewModel.onAspectRatioToggle() },
                onPeakingToggle = { viewModel.onPeakingToggle() },
                onZebraToggle = { viewModel.onZebraToggle() },
                onGridToggle = { viewModel.onGridToggle() },
                onTiltMeterToggle = { viewModel.onTiltMeterToggle() }
            )

            // 2. Viewfinder Section with GL Surface and Overlays
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Real-time OpenGL ES Preview with Shader filters
                AndroidView(
                    factory = { context ->
                        CameraGlView(context) { surface ->
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
                    onFocusPointSelected = { /* Manual AF target point */ }
                )

                // Tiltmeter / Artificial Horizon Level
                if (uiState.isTiltMeterEnabled) {
                    TiltMeterWidget(
                        levelOrientation = levelState,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Live Luminance Histogram Overlay (Top Right)
                if (uiState.isHistogramEnabled) {
                    HistogramWidget(
                        histogramData = histogramData,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    )
                }
            }

            // 3. Mechanical Slider Wheel (ISO / Shutter / Focus / WB / EV)
            ManualControlDial(
                state = uiState,
                onIsoChanged = { iso, isAuto -> viewModel.onIsoChanged(iso, isAuto) },
                onShutterChanged = { nanos, isAuto -> viewModel.onShutterChanged(nanos, isAuto) },
                onFocusChanged = { dist, isAuto -> viewModel.onFocusChanged(dist, isAuto) },
                onKelvinChanged = { k, isAuto -> viewModel.onKelvinChanged(k, isAuto) },
                onEvChanged = { ev -> viewModel.onEvChanged(ev) }
            )

            // 4. Quick Parameter Switch Bar [ISO] [SEC] [FOCUS] [WB] [EV]
            ManualParamSelectorBar(
                state = uiState,
                onParamSelected = { param -> viewModel.onParamSelected(param) }
            )

            // 5. Shooting Mode Selector (PHOTO, MANUAL, SLOW SHUTTER, BURST, VIDEO)
            ModeSelector(
                selectedMode = uiState.selectedMode,
                onModeSelected = { mode -> viewModel.onModeSelected(mode) }
            )

            // 6. Tactical Shutter & Gallery Controls
            ShutterControlSection(
                mode = uiState.selectedMode,
                isRecording = uiState.isRecordingVideo,
                onShutterClick = { viewModel.onShutterPressed() },
                onGalleryClick = { /* Open photo gallery */ },
                onFlipCameraClick = { /* Switch front/back camera */ }
            )
        }
    }
}
