package com.example.flix360.ui.dashboard

import android.os.Bundle
import android.widget.ArrayAdapter
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.flix360.core.RetrofitClient
import com.example.flix360.core.SessionManager
import com.example.flix360.data.remote.dto.InventoryAssetDto
import com.example.flix360.data.remote.dto.ProductDto
import com.example.flix360.data.remote.dto.CategoryDto
import com.example.flix360.data.remote.dto.LocationDto
import com.example.flix360.databinding.ActivityDashboardBinding
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var session: SessionManager

    private var locations: List<LocationDto> = emptyList()
    private val summaryAdapter = CategorySummaryAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        RetrofitClient.attachSessionManager(session)

        // RecyclerView setup
        if (binding.rvInventorySummary.layoutManager == null) {
            binding.rvInventorySummary.layoutManager = GridLayoutManager(this, 2)
        }
        binding.rvInventorySummary.adapter = summaryAdapter

        fetchLocations()

        // Captura (botón central)
        binding.btnNavCapture.setOnClickListener {
            startActivity(android.content.Intent(this, com.example.flix360.ui.scanner.ScannerActivity::class.java))
        }
        // Auditoría (botón derecho)
        binding.btnNavAudit.setOnClickListener {
            startActivity(android.content.Intent(this, com.example.flix360.ui.audit.AuditActivity::class.java))
        }
    }

    private fun fetchLocations() {
        lifecycleScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) { RetrofitClient.api.getLocations() }
                if (resp.isSuccessful) {
                    locations = resp.body()?.data.orEmpty()
                    if (locations.isEmpty()) {
                        Toast.makeText(this@DashboardActivity, "Sin locales", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    setupLocationDropdown(locations)
                    val activeId = session.getActiveLocationId()
                    val selected = locations.firstOrNull { it.id == activeId } ?: locations.first()
                    setSelectedLocation(selected)
                } else {
                    Toast.makeText(this@DashboardActivity, "Fallo cargando locales (${resp.code()})", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DashboardActivity, "Modo Offline: Mostrando últimos datos conocidos", Toast.LENGTH_LONG).show()
                // Si tuviéramos cache local, la cargaríamos aquí. Por ahora solo queda el spinner vacío o último guardado.
            }
        }
    }

    private fun setupLocationDropdown(items: List<LocationDto>) {
        val dropdown: MaterialAutoCompleteTextView = binding.dropdownLocations
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items.map { it.name })
        dropdown.setAdapter(adapter)
        dropdown.setOnItemClickListener { parent, _, position, _ ->
            val loc = items[position]
            setSelectedLocation(loc)
        }
        // Preseleccionar valor actual si existe
        val activeId = session.getActiveLocationId()
        val idx = items.indexOfFirst { it.id == activeId }.takeIf { it >= 0 } ?: 0
        dropdown.setText(items[idx].name, false)
    }

    private fun setSelectedLocation(loc: LocationDto) {
        // Guardar en sesión
        session.saveActiveLocationId(loc.id)
        // Actualizar UI
        // El nombre del local lo muestra el Spinner seleccionado
        // Consultar inventario del local
        fetchInventoryForLocation(loc.id)
    }

    private fun fetchInventoryForLocation(locationId: String) {
        lifecycleScope.launch {
            try {
                val api = RetrofitClient.api
                val assetsDeferred = async(Dispatchers.IO) { api.getInventoryAssets(locationId) }
                val productsDeferred = async(Dispatchers.IO) { api.getProducts() }
                val categoriesDeferred = async(Dispatchers.IO) { api.getCategories() }

                val assetsResp = assetsDeferred.await()
                val productsResp = productsDeferred.await()
                val categoriesResp = categoriesDeferred.await()

                if (assetsResp.isSuccessful) {
                    val assets: List<InventoryAssetDto> = assetsResp.body()?.data.orEmpty()
                    val total = assets.sumOf { it.quantity }
                    binding.stockNumber.text = java.text.NumberFormat.getNumberInstance(java.util.Locale.getDefault()).format(total) + " u."

                    val products: Map<String, ProductDto> = if (productsResp.isSuccessful)
                        productsResp.body()?.data.orEmpty().associateBy { it.id } else emptyMap<String, ProductDto>()
                    val categories: Map<String, CategoryDto> = if (categoriesResp.isSuccessful)
                        categoriesResp.body()?.data.orEmpty().associateBy { it.id } else emptyMap<String, CategoryDto>()

                    // Agrupar por nombre de categoría
                    val grouped: Map<String, Double> = assets.groupBy { asset ->
                        val product = asset.productId.let { products[it] }
                        val catName = product?.categoryId?.let { categories[it]?.name }
                        catName ?: "SIN CATEGORÍA"
                    }.mapValues { (_, list) -> list.sumOf { it.quantity } }

                    val summaries = grouped.entries.map { (name, qty) -> CategorySummary(name, qty) }
                    summaryAdapter.submit(summaries)
                } else {
                    Toast.makeText(this@DashboardActivity, "Fallo cargando stock (${assetsResp.code()})", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DashboardActivity, "Modo Offline: Mostrando últimos datos conocidos", Toast.LENGTH_LONG).show()
            }
        }
    }
}

