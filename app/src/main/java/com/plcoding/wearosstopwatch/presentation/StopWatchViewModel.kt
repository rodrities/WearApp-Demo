@file:OptIn(ExperimentalCoroutinesApi::class)

package com.plcoding.wearosstopwatch.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import android.util.Log
import okhttp3.OkHttpClient
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


class StopWatchViewModel: ViewModel() {
    private val TAG = "MiApp"
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://color.serialif.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(getUnsafeOkHttpClient())
        .build()
    private val pausedTimes = mutableListOf<Long>()
    private val _elapsedTime = MutableStateFlow(0L)
    private val _timerState = MutableStateFlow(TimerState.RESET)
    val timerState = _timerState.asStateFlow()

    private val _heartRateText = MutableStateFlow("Frecuencia cardíaca: - BPM")
    val heartRateText: StateFlow<String> = _heartRateText

    fun updateHeartRateText(newText: String) {
        _heartRateText.value = newText
    }

    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss:SSS")
    val stopWatchText = _elapsedTime
        .map { millis ->
            LocalTime.ofNanoOfDay(millis * 1_000_000).format(formatter)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            "00:00:00:000"
        )

    init {
        _timerState
            .flatMapLatest { timerState ->
                getTimerFlow(
                    isRunning = timerState == TimerState.RUNNING
                )
            }
            .onEach { timeDiff ->
                _elapsedTime.update { it + timeDiff }
            }
            .launchIn(viewModelScope)
    }


    fun toggleIsRunning() {
        when (timerState.value) {
            TimerState.RUNNING -> {
                _timerState.update { TimerState.PAUSED }
                pausedTimes.add(_elapsedTime.value)
            }
            TimerState.PAUSED,
            TimerState.RESET -> _timerState.update {
                TimerState.RUNNING

            }
        }
    }


    fun resetTimer() {
        Log.e(TAG, "Resetiendo TIMER")
        _timerState.update { TimerState.RESET }
        _elapsedTime.update { 0L }
    }

    private fun getTimerFlow(isRunning: Boolean): Flow<Long> {
        return flow {
            var startMillis = System.currentTimeMillis()
            while(isRunning) {
                val currentMillis = System.currentTimeMillis()
                val timeDiff = if(currentMillis > startMillis) {
                    currentMillis - startMillis
                } else 0L
                emit(timeDiff)
                startMillis = System.currentTimeMillis()
                delay(10L)
            }
        }
    }

    suspend fun enviarTiemposAPI() {
        Log.e(TAG, "Enviar TIEMPOS")
        Log.e(TAG,pausedTimes.toString() )
        val apiService = retrofit.create(ApiService::class.java)
        try {
            val response = apiService.enviarTiempos()
            if (response.isSuccessful) {
                // Éxito: los tiempos se enviaron correctamente
                Log.e(TAG, "Mensaje SI")
                Log.e(TAG, response.body().toString())
            } else {
                Log.e(TAG, "Mensaje ERROR2")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al enviar los tiempos: ${e.message}", e)
        }
    }

    private fun getUnsafeOkHttpClient(): OkHttpClient {
        try {
            Log.e(TAG, "ENTRANDO TRY UNSAFE")
            // Crea un trust manager que no valida certificados
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                }

                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }
            })

            // Crea un SSL context que ignora la validación de certificados
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            // Crea un OkHttpClient que use el SSL context sin validación de certificados
            val builder = OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }

            return builder.build()
        } catch (e: Exception) {
            throw RuntimeException(e)
            Log.e(TAG, "CATCH TRY UNSAFE")
        }
    }


}