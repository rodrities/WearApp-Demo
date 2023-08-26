package com.plcoding.wearosstopwatch.presentation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.*
import kotlinx.coroutines.launch
import kotlin.reflect.KSuspendFunction0

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel = viewModel<StopWatchViewModel>()
            val timerState by viewModel.timerState.collectAsStateWithLifecycle()
            val stopWatchText by viewModel.stopWatchText.collectAsStateWithLifecycle()

            val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

            val heartRateListener = object : SensorEventListener {
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    // No se necesita implementar en este caso
                }

                override fun onSensorChanged(event: SensorEvent?) {
                    if (event?.sensor?.type == Sensor.TYPE_HEART_RATE) {
                        val heartRate = event.values[0]
                        viewModel.updateHeartRateText("Frecuencia cardíaca: $heartRate BPM")
                    }
                }
            }

            DisposableEffect(heartRateSensor) {
                sensorManager.registerListener(heartRateListener, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL)
                onDispose {
                    sensorManager.unregisterListener(heartRateListener)
                }
            }

            Scaffold(
                timeText = {
                    TimeText(
                        timeTextStyle = TimeTextDefaults.timeTextStyle(
                            fontSize = 10.sp
                        )
                    )
                },
                vignette = {
                    Vignette(vignettePosition = VignettePosition.TopAndBottom)
                }
            ) {
                StopWatch(
                    state = timerState,
                    text = stopWatchText,
                    onToggleRunning = viewModel::toggleIsRunning,
                    onReset = viewModel::resetTimer,
                    onEnviar = viewModel::enviarTiemposAPI, // Llamada suspendida aquí
                    modifier = Modifier.fillMaxSize()
                )
            }
        }


    }
}

@Composable
private fun StopWatch(
    state: TimerState,
    text: String,
    onToggleRunning: () -> Unit,
    onReset: () -> Unit,
    onEnviar: KSuspendFunction0<Unit>,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val viewModel = viewModel<StopWatchViewModel>()

    val heartRateText by viewModel.heartRateText.collectAsState()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Text(
            text = heartRateText, // Muestra la frecuencia cardíaca aquí
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(onClick = onToggleRunning) {
                Icon(
                    imageVector = if (state == TimerState.RUNNING) {
                        Icons.Default.Pause
                    } else {
                        Icons.Default.PlayArrow
                    },
                    contentDescription = null
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onReset,
                enabled = state != TimerState.RESET,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.surface
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = null
                )
            }
            Spacer(modifier = Modifier.width(8.dp))


            Button(
                onClick = {
                    coroutineScope.launch {
                        onEnviar() // Llamada a la función suspendida dentro del ámbito de coroutine
                    }
                },
                enabled = state != TimerState.RESET,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.surface
                )
            ) {
                Text(
                    text = "Enviar",
                    fontWeight = FontWeight.Bold
                )

            }
        }
    }
}
