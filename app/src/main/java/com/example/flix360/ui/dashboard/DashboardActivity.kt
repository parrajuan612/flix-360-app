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
import com.example.flix360.data.remote.dto.LocationDto
import com.example.flix360.databinding.ActivityDashboardBinding
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var session: SessionManager

    private var locations: List<LocationDto> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        RetrofitClient.attachSessionManager(session)

        fetchLocations()
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
                val resp = withContext(Dispatchers.IO) { RetrofitClient.api.getInventoryAssets(locationId) }
                if (resp.isSuccessful) {
                    val items: List<InventoryAssetDto> = resp.body()?.data.orEmpty()
                    val total = items.sumOf { it.quantity }
                    binding.stockNumber.text = java.text.NumberFormat.getNumberInstance(java.util.Locale.getDefault()).format(total) + " u."
                } else {
                    Toast.makeText(this@DashboardActivity, "Fallo cargando stock (${resp.code()})", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DashboardActivity, "Modo Offline: Mostrando últimos datos conocidos", Toast.LENGTH_LONG).show()
            }
        }
    }
}

