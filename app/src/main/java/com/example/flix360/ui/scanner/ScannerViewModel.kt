package com.example.flix360.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flix360.core.RetrofitClient
import com.example.flix360.core.SeuicManager
import com.example.flix360.data.remote.dto.AssetRequest
import com.example.flix360.data.remote.dto.RfidTagRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random
import android.media.ToneGenerator
import android.media.AudioManager
import android.util.Log

class ScannerViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {

    private val toneGen: ToneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

    init {
        // Inicializa hardware (si está disponible) sin bloquear
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            SeuicManager.init()
        }
    }

    data class UiState(
        val isScanning: Boolean = false,
        val txPower: Int = 30,
        val scannedTags: List<ScannedTag> = emptyList(),
        val scanCount: Int = 0,
        val selectedProductId: String? = null,
        val selectedLocationId: String? = null,
        val isSaving: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var scanJob: Job? = null

    fun setTxPower(power: Int) {
        _state.update { it.copy(txPower = power) }
        if (SeuicManager.isSupported) {
            SeuicManager.setPower(power)
        }
    }
    fun setSelectedProductId(id: String?) { _state.update { it.copy(selectedProductId = id) } }
    fun setSelectedLocationId(id: String?) { _state.update { it.copy(selectedLocationId = id) } }

    // Toggle queda disponible, pero la UI usará push-to-hold (start/stop públicos)
    fun toggleScanner() { if (_state.value.isScanning) stopScanning() else startScanning() }

    // Push-to-hold: públicos
    fun startScanning() {
        if (_state.value.isScanning) return
        if (SeuicManager.isSupported) {
            if (SeuicManager.startScanning()) startHardwarePolling() else startSimulatedScan()
        } else {
            startSimulatedScan()
        }
    }

    fun stopScanning() {
        if (!_state.value.isScanning) return
        if (SeuicManager.isSupported) SeuicManager.stopScanning()
        stopScan()
    }

    private fun startSimulatedScan() {
        _state.update { it.copy(isScanning = true) }
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            while (_state.value.isScanning) {
                delay(300)
                val epc = randomEpc()
                val rssi = Random.nextInt(-80, -39)
                _state.update { current ->
                    val exists = current.scannedTags.any { it.epc == epc }
                    val newList = if (exists) current.scannedTags else current.scannedTags + ScannedTag(epc, rssi)
                    if (!exists) runCatching { toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 50) }
                    current.copy(
                        scannedTags = newList,
                        scanCount = if (!exists) current.scanCount + 1 else current.scanCount
                    )
                }
            }
        }
    }

    private fun stopScan() {
        _state.update { it.copy(isScanning = false) }
        scanJob?.cancel(); scanJob = null
    }

    private fun startHardwarePolling() {
        _state.update { it.copy(isScanning = true) }
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            while (_state.value.isScanning && SeuicManager.isSupported) {
                val epcs = SeuicManager.getTags()
                if (epcs.isNotEmpty()) {
                    _state.update { current ->
                        var list = current.scannedTags
                        epcs.forEach { epc ->
                            if (list.none { it.epc.equals(epc, ignoreCase = true) }) {
                                list = list + ScannedTag(epc.uppercase(), -50)
                                runCatching { toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 50) }
                            }
                        }
                        current.copy(scannedTags = list, scanCount = list.size)
                    }
                }
                delay(200)
            }
        }
    }

    // Guardar asignación: registra tags y crea assets
    fun saveAssignation(onResult: (Boolean, String?) -> Unit) {
        val productId = _state.value.selectedProductId
        val locationId = _state.value.selectedLocationId
        val tags = _state.value.scannedTags
        if (productId.isNullOrBlank() || locationId.isNullOrBlank() || tags.isEmpty()) {
            onResult(false, "Faltan datos obligatorios"); return
        }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val api = RetrofitClient.api
                for (tag in tags) {
                    val tagResp = api.createRfidTag(RfidTagRequest(tag.epc))
                    if (!tagResp.isSuccessful) {
                        val errBody = tagResp.errorBody()?.string()
                        val friendly = mapApiError(tagResp.code(), errBody)
                        Log.e("API_ERROR", "RFID: $errBody")
                        _state.update { it.copy(isSaving = false) }
                        onResult(false, friendly)
                        return@launch
                    }
                    val rfidId = tagResp.body()!!.id
                    val assetResp = api.createInventoryAsset(
                        AssetRequest(
                            productId = productId,
                            locationId = locationId,
                            rfidTagId = rfidId,
                            quantity = 1.0,
                            inventoryMode = "unit"
                        )
                    )
                    if (!assetResp.isSuccessful) {
                        val errBody = assetResp.errorBody()?.string()
                        val friendly = mapApiError(assetResp.code(), errBody)
                        Log.e("API_ERROR", "ASSET: $errBody")
                        _state.update { it.copy(isSaving = false) }
                        onResult(false, friendly)
                        return@launch
                    }
                }
                _state.update { it.copy(isSaving = false) }
                clearScans()
                onResult(true, null)
            } catch (e: Exception) {
                // Offline-first: encola en Room como pending scan
                try {
                    val ctx = getApplication<android.app.Application>().applicationContext
                    com.example.flix360.data.repo.PendingScanRepository(ctx).enqueue(
                        productId = productId,
                        locationId = locationId,
                        epcs = tags.map { it.epc }
                    )
                    _state.update { it.copy(isSaving = false) }
                    clearScans()
                    onResult(true, "Guardado localmente. Pendiente de sincronización")
                } catch (ee: Exception) {
                    _state.update { it.copy(isSaving = false) }
                    onResult(false, e.localizedMessage)
                }
            }
        }
    }

    fun clearScans() { _state.update { it.copy(scannedTags = emptyList(), scanCount = 0) } }

    private fun mapApiError(code: Int, body: String?): String {
        if (code == 401) return "Tu sesión ha expirado."
        val msg = body.orEmpty()
        return if (msg.contains("23505", true) || msg.contains("duplicate key", true)) {
            "Error: Una o más etiquetas de este lote ya se encuentran asignadas en el inventario."
        } else {
            if (msg.isBlank()) "Error del servidor" else "Error del servidor: $msg"
        }
    }

    override fun onCleared() {
        super.onCleared()
        runCatching { toneGen.release() }
    }

    private fun randomEpc(): String {
        val prefix = "3400E"
        val hex = (1..16).joinToString("") { Random.nextInt(0, 16).toString(16).uppercase() }
        return prefix + hex
    }
}
