package com.example.flix360.ui.scanner

import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import com.example.flix360.data.repo.ProductRepository
import com.example.flix360.data.repo.LocationRepository

fun ScannerActivity.loadDropdownData() {
    val dropdownLocal = binding.dropdownLocal
    val dropdownProduct = binding.dropdownProduct
    lifecycleScope.launch {
        val prodRepo = com.example.flix360.data.repo.ProductRepository(this@loadDropdownData)
        val locRepo = com.example.flix360.data.repo.LocationRepository(this@loadDropdownData)
        try {
            val prodDeferred = async(Dispatchers.IO) { prodRepo.getProducts() }
            val locDeferred = async(Dispatchers.IO) { locRepo.getLocations() }
            val prod = prodDeferred.await()
            val locs = locDeferred.await()

            dropdownProduct.setAdapter(ArrayAdapter(this@loadDropdownData, android.R.layout.simple_list_item_1, prod.map { it.name }))
            dropdownLocal.setAdapter(ArrayAdapter(this@loadDropdownData, android.R.layout.simple_list_item_1, locs.map { it.name }))

            dropdownProduct.setOnItemClickListener { v, _, position, _ ->
                viewModel.setSelectedProductId(prod.getOrNull(position)?.id)
                viewModel.stopScanning(); viewModel.clearScans()
                v.clearFocus()
                val imm = this@loadDropdownData.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
            }
            dropdownLocal.setOnItemClickListener { v, _, position, _ ->
                viewModel.setSelectedLocationId(locs.getOrNull(position)?.id)
                viewModel.stopScanning(); viewModel.clearScans()
                v.clearFocus()
                val imm = this@loadDropdownData.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
            }

            if (prod.isEmpty() || locs.isEmpty()) {
                android.widget.Toast.makeText(this@loadDropdownData, "No hay datos locales. Conéctate para sincronizar", android.widget.Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            android.widget.Toast.makeText(this@loadDropdownData, "No hay datos locales. Conéctate para sincronizar", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
