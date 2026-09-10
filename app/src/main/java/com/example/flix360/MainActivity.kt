package com.example.flix360

import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.flix360.core.RetrofitClient
import com.example.flix360.core.SessionManager
import com.example.flix360.data.remote.dto.LoginRequest
import com.example.flix360.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        // Restaurar sesión si existe
        if (session.isSessionValid()) {
            Toast.makeText(this, "Sesión restaurada (Offline/Online)", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, com.example.flix360.ui.dashboard.DashboardActivity::class.java))
            finish()
            return
        }

        binding.btnSend.setOnClickListener {
            val user = binding.inputUser.text?.toString()?.trim().orEmpty()
            val pass = binding.inputPassword.text?.toString()?.trim().orEmpty()
            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Ingrese usuario y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    binding.btnSend.isEnabled = false
                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.api.login(LoginRequest(user, pass))
                    }
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (!body?.token.isNullOrBlank()) {
                            session.saveToken(body!!.token)
                            Toast.makeText(this@MainActivity, "Login exitoso", Toast.LENGTH_LONG).show()

                            startActivity(Intent(this@MainActivity, com.example.flix360.ui.dashboard.DashboardActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this@MainActivity, "Respuesta inválida del servidor", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Error de autenticación (${response.code()})", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Fallo de red: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                } finally {
                    binding.btnSend.isEnabled = true
                }
            }
        }
    }
}