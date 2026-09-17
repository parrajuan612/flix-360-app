package com.example.flix360.ui.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import com.example.flix360.core.RetrofitClient
import com.example.flix360.core.SeuicManager
import android.media.ToneGenerator
import android.media.AudioManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuditViewModel : ViewModel() {

    private val toneGen: ToneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

    data class UiState(
        val isScanning: Boolean = false,
        val expectedTags: Set<String> = emptySet(),
        val scannedTags: Set<String> = emptySet(),
        val missing: Set<String> = emptySet(),
        val extra: Set<String> = emptySet()
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var scanJob: Job? = null

    fun startScanning() {
        if (_state.value.isScanning) return
        _state.update { it.copy(isScanning = true) }
        android.util.Log.d("AUDIT_HW", "Intentando encender láser")
        scanJob?.cancel()
        scanJob = viewModelScope.launch(Dispatchers.IO) {
            // Iniciar hardware si está disponible; si no, simulador
            val hwReady = SeuicManager.init() && SeuicManager.startScanning()
            if (hwReady) {
                while (_state.value.isScanning) {
                    val epcs = SeuicManager.getTags()
                    if (epcs.isNotEmpty()) {
                        _state.update { cur ->
                            val new = epcs.fold(cur.scannedTags) { acc, e -> if (acc.any { it.equals(e, ignoreCase = true) }) acc else acc + e.uppercase() }
                            if (new.size > cur.scannedTags.size) runCatching { toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 50) }
                            cur.copy(scannedTags = new)
                        }
                        recompute()
                    }
                    delay(100)
                }
                SeuicManager.stopScanning()
            } else {
                while (_state.value.isScanning) {
                    delay(500)
                    recompute()
                }
            }
        }
    }

    fun stopScanning() {
        if (!_state.value.isScanning) return
        _state.update { it.copy(isScanning = false) }
        runCatching { SeuicManager.stopScanning() }
        scanJob?.cancel(); scanJob = null
    }

    fun setExpected(tags: Set<String>) { _state.update { it.copy(expectedTags = tags) }.also { recompute() } }
    fun addScanned(epc: String) { _state.update { it.copy(scannedTags = it.scannedTags + epc.uppercase()) }.also { recompute() } }
    fun clearScanned() { _state.update { it.copy(scannedTags = emptySet(), missing = emptySet(), extra = emptySet()) } }

    private fun recompute() {
        val expected = _state.value.expectedTags
        val scanned = _state.value.scannedTags
        val missing = expected - scanned
        val extra = scanned - expected
        _state.update { it.copy(missing = missing, extra = extra) }
    }

    fun fetchExpectedInventory(locationId: String, productId: String? = null) {
        viewModelScope.launch {
            try {
                val api = RetrofitClient.api
                val resp = api.getInventoryAssets(locationId)
                if (resp.isSuccessful) {
                    val assets = resp.body()?.data.orEmpty()
                    val filtered = if (!productId.isNullOrBlank()) assets.filter { it.productId == productId } else assets
                    val tags: Set<String> = filtered.mapNotNull { it.epc?.uppercase() ?: it.rfidTagId }
                        .toSet()
                    _state.update { it.copy(expectedTags = tags) }
                    recompute()
                }
            } catch (_: Exception) { }
        }
    }
}
