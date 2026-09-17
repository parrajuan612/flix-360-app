package com.example.flix360.ui.audit

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import com.example.flix360.databinding.ActivityAuditBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AuditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuditBinding
    private val viewModel: AuditViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Dropdowns Offline-First (Locales / Productos)
        val dropdownLocal = binding.dropdownLocal
        val dropdownProduct = binding.dropdownProduct
        var selectedLocationId: String? = null
        var selectedProductId: String? = null
        lifecycleScope.launch {
            val prodRepo = com.example.flix360.data.repo.ProductRepository(this@AuditActivity)
            val locRepo = com.example.flix360.data.repo.LocationRepository(this@AuditActivity)
            try {
                val prodDeferred = async(Dispatchers.IO) { prodRepo.getProducts() }
                val locDeferred = async(Dispatchers.IO) { locRepo.getLocations() }
                val prod = prodDeferred.await()
                val locs = locDeferred.await()
                dropdownProduct.setAdapter(android.widget.ArrayAdapter(this@AuditActivity, android.R.layout.simple_list_item_1, prod.map { it.name }))
                dropdownLocal.setAdapter(android.widget.ArrayAdapter(this@AuditActivity, android.R.layout.simple_list_item_1, locs.map { it.name }))
                dropdownProduct.setOnItemClickListener { v, _, position, _ ->
                    selectedProductId = prod.getOrNull(position)?.id
                    selectedLocationId?.let { viewModel.fetchExpectedInventory(it, selectedProductId) }
                    v.clearFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
                dropdownLocal.setOnItemClickListener { v, _, position, _ ->
                    selectedLocationId = locs.getOrNull(position)?.id
                    viewModel.clearScanned()
                    selectedLocationId?.let { viewModel.fetchExpectedInventory(it, selectedProductId) }
                    v.clearFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(this@AuditActivity, "No hay datos locales. Conéctate para sincronizar", android.widget.Toast.LENGTH_LONG).show()
            }
        }

        // Observa y pinta contadores
        lifecycleScope.launchWhenStarted {
            viewModel.state.collectLatest { ui ->
                binding.txtExpected.text = "Esperados: ${ui.expectedTags.size}"
                binding.txtRead.text = "Leídos: ${ui.scannedTags.size}"
                binding.txtDiff.text = "Diferencia: ${(ui.scannedTags - ui.expectedTags).size}"
            }
        }

        // Bottom nav navigation
        binding.btnNavAudit.setOnClickListener {
            // ya estamos aquí
        }
        binding.btnNavCapture.setOnClickListener {
            startActivity(android.content.Intent(this, com.example.flix360.ui.scanner.ScannerActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // Asegurar que el HW esté inicializado al entrar
        kotlin.runCatching { com.example.flix360.core.SeuicManager.init() }
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
                android.util.Log.d("AUDIT_HW", "Gatillo presionado (ACTION_DOWN)")
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
