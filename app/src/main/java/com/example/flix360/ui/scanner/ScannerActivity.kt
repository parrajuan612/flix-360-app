package com.example.flix360.ui.scanner

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import com.example.flix360.core.SessionManager
import com.example.flix360.databinding.ActivityScannerBinding
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.EditText
import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.util.Log

class ScannerActivity : AppCompatActivity() {

    internal lateinit var binding: ActivityScannerBinding
    private lateinit var session: SessionManager
    private val adapter = ScannedTagAdapter()
    internal val viewModel: ScannerViewModel by lazy { androidx.lifecycle.ViewModelProvider(this)[ScannerViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        // Top bar fijo para flujo de asignación
        binding.txtSubtitle.text = "Asignación"

        // RecyclerView vacío por ahora
        binding.rvScannedTags.layoutManager = LinearLayoutManager(this)
        binding.rvScannedTags.adapter = adapter

        // Bind UI to ViewModel state
        lifecycleScope.launchWhenStarted {
            viewModel.state.collectLatest { ui ->
                val range = when {
                    ui.txPower <= 10 -> "Rango: Corto (0 - 30 cm)"
                    ui.txPower <= 20 -> "Rango: Medio (1 - 3 metros)"
                    else -> "Rango: Largo (Hasta 10 metros)"
                }
                binding.txtPowerValue.text = range
                binding.txtTagCount.text = ui.scanCount.toString()
                binding.txtScanState.text = if (ui.isScanning) "ESCANEANDO" else "LISTO"
                binding.txtScanState.setTextColor(android.graphics.Color.parseColor(if (ui.isScanning) "#16D98C" else "#8FA1AE"))
                adapter.submit(ui.scannedTags)

                // Botón Asignar
                binding.btnAssign.text = if (ui.isSaving) "GUARDANDO..." else "ASIGNAR ${ui.scanCount} PRODUCTOS"
                binding.btnAssign.isEnabled = !ui.isScanning && ui.scanCount > 0 && !ui.isSaving
                binding.btnAssign.visibility = if (!ui.isScanning && ui.scanCount > 0) android.view.View.VISIBLE else android.view.View.GONE
            }
        }

        // Slider controla rango (no mostramos dBm)
        binding.sliderPower.addOnChangeListener { _, value, _ ->
            viewModel.setTxPower(value.toInt())
        }
        binding.sliderPower.value = 30f

        // Cargar Locales y Productos y preparar dropdowns
        loadDropdownData()

        // Gatillo virtual con validación de producto seleccionado
        binding.cardFeedback.setOnClickListener {
            if (viewModel.state.value.selectedProductId.isNullOrBlank()) {
                android.widget.Toast.makeText(this, "Seleccione un producto para iniciar", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                viewModel.toggleScanner()
            }
        }

        // Acción Guardar/Asignar
        binding.btnAssign.setOnClickListener {
            viewModel.saveAssignation { ok, msg ->
                android.widget.Toast.makeText(this, if (ok) "Asignación Exitosa" else ("Error: " + (msg ?: "desconocido")), android.widget.Toast.LENGTH_LONG).show()
            }
        }

        // Bottom nav navigation
        binding.btnNavAudit.setOnClickListener {
            startActivity(android.content.Intent(this, com.example.flix360.ui.audit.AuditActivity::class.java))
        }
        binding.btnNavCapture.setOnClickListener {
            // ya estamos aquí; opcionalmente podríamos refrescar
        }

        // Bottom nav: volver a Locales
        binding.btnBottomLocales.setOnClickListener { finish() }
        //
    }

    override fun onPause() {
        super.onPause()
        // Liberar HW al salir de Captura para permitir uso en Auditoría
        kotlin.runCatching {
            if (viewModel.state.value.isScanning) viewModel.stopScanning()
            com.example.flix360.core.SeuicManager.stopScanning()
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = android.graphics.Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        val keyCode = event.keyCode
        // Keycodes del gatillo Seuic (138, 139, 280, 293)
        if (keyCode == 138 || keyCode == 139 || keyCode == 280 || keyCode == 293) {
            if (event.action == android.view.KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                viewModel.startScanning()
                return true
            } else if (event.action == android.view.KeyEvent.ACTION_UP) {
                viewModel.stopScanning()
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
