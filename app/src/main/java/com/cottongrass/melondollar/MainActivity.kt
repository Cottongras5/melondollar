package com.cottongrass.melondollar

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var rateTextView: TextView
        private lateinit var lastUpdatedTextView: TextView
            private lateinit var refreshButton: Button
                private lateinit var bsInput: EditText
                    private lateinit var usdInput: EditText
                        private lateinit var convertButton: Button

                            private var currentRate: Double = 0.0

                                // Track which field was last edited: "bs" or "usd"
                                private var lastEdited: String = ""

                                    data class DolarRate(
                                        val promedio: Double?,
                                        val fechaActualizacion: String?,
                                        val nombre: String?
                                    )

                                    override fun onCreate(savedInstanceState: Bundle?) {
                                        super.onCreate(savedInstanceState)
                                        setContentView(R.layout.activity_main)

                                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

                                        rateTextView = findViewById(R.id.rateTextView)
                                        lastUpdatedTextView = findViewById(R.id.lastUpdatedTextView)
                                        refreshButton = findViewById(R.id.refreshButton)
                                        bsInput = findViewById(R.id.bsInput)
                                        usdInput = findViewById(R.id.usdInput)
                                        convertButton = findViewById(R.id.convertButton)

                                        // Add text watchers to track last edited field
                                        bsInput.addTextChangedListener(object : TextWatcher {
                                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                                if (s != null && s.isNotEmpty()) lastEdited = "bs"
                                            }
                                            override fun afterTextChanged(s: Editable?) {}
                                        })

                                        usdInput.addTextChangedListener(object : TextWatcher {
                                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                                if (s != null && s.isNotEmpty()) lastEdited = "usd"
                                            }
                                            override fun afterTextChanged(s: Editable?) {}
                                        })

                                        fetchRate()

                                        refreshButton.setOnClickListener { fetchRate() }

                                        convertButton.setOnClickListener {
                                            if (currentRate <= 0) {
                                                Toast.makeText(this, "Ya va que no ha cargado la tasa", Toast.LENGTH_SHORT).show()
                                                return@setOnClickListener
                                            }

                                            when (lastEdited) {
                                                "bs" -> {
                                                    val bsText = bsInput.text.toString()
                                                    if (bsText.isEmpty()) {
                                                        Toast.makeText(this, "Pon un número en bolívares", Toast.LENGTH_SHORT).show()
                                                        return@setOnClickListener
                                                    }
                                                    val bs = bsText.toDoubleOrNull()
                                                    if (bs == null) {
                                                        Toast.makeText(this, "No te alcanza ni para una empanada", Toast.LENGTH_SHORT).show()
                                                        return@setOnClickListener
                                                    }
                                                    val usd = bs / currentRate
                                                    usdInput.setText(String.format("%.2f", usd))
                                                }
                                                "usd" -> {
                                                    val usdText = usdInput.text.toString()
                                                    if (usdText.isEmpty()) {
                                                        Toast.makeText(this, "Pon un número en dólares", Toast.LENGTH_SHORT).show()
                                                        return@setOnClickListener
                                                    }
                                                    val usd = usdText.toDoubleOrNull()
                                                    if (usd == null) {
                                                        Toast.makeText(this, "No te alcanza ni para la harina pan", Toast.LENGTH_SHORT).show()
                                                        return@setOnClickListener
                                                    }
                                                    val bs = usd * currentRate
                                                    bsInput.setText(String.format("%.2f", bs))
                                                }
                                                else -> {
                                                    // If neither field was touched, fallback to old behavior: check which one has content
                                                    val bsText = bsInput.text.toString()
                                                    val usdText = usdInput.text.toString()
                                                    when {
                                                        bsText.isNotEmpty() -> {
                                                            val bs = bsText.toDoubleOrNull()
                                                            if (bs != null) {
                                                                val usd = bs / currentRate
                                                                usdInput.setText(String.format("%.2f", usd))
                                                                lastEdited = "bs"
                                                            } else {
                                                                Toast.makeText(this, "No te alcanza ni para el pasaje", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                        usdText.isNotEmpty() -> {
                                                            val usd = usdText.toDoubleOrNull()
                                                            if (usd != null) {
                                                                val bs = usd * currentRate
                                                                bsInput.setText(String.format("%.2f", bs))
                                                                lastEdited = "usd"
                                                            } else {
                                                                Toast.makeText(this, "No te alcanza ni para vender dólares", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                        else -> {
                                                            Toast.makeText(this, "Pon algo en cualquier lado", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    private fun fetchRate() {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            try {
                                                val client = OkHttpClient()
                                                val request = Request.Builder()
                                                .url("https://ve.dolarapi.com/v1/dolares/oficial")
                                                .get()
                                                .build()
                                                val response = client.newCall(request).execute()
                                                if (response.isSuccessful) {
                                                    val body = response.body?.string()
                                                    if (body != null) {
                                                        val gson = Gson()
                                                        val rate = gson.fromJson(body, DolarRate::class.java)
                                                        val promedio = rate.promedio ?: 0.0
                                                        if (promedio > 0) {
                                                            currentRate = promedio
                                                            val fecha = rate.fechaActualizacion ?: ""
                                                            val nombre = rate.nombre ?: "Oficial"
                                                            withContext(Dispatchers.Main) {
                                                                rateTextView.text = String.format("%.2f Bs", promedio)
                                                                val formattedDate = if (fecha.isNotEmpty()) {
                                                                    try {
                                                                        val datePart = fecha.split("T").firstOrNull() ?: fecha
                                                                        val parts = datePart.split("-")
                                                                        if (parts.size == 3) {
                                                                            "${parts[2]}/${parts[1]}/${parts[0]}"
                                                                        } else {
                                                                            fecha
                                                                        }
                                                                    } catch (_: Exception) {
                                                                        fecha
                                                                    }
                                                                } else "--"
                                                                    lastUpdatedTextView.text = "Última actualización: $formattedDate"
                                                            }
                                                        } else {
                                                            withContext(Dispatchers.Main) {
                                                                rateTextView.text = "Error: esta tasa no es válida"
                                                                lastUpdatedTextView.text = "Última actualización: --"
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    withContext(Dispatchers.Main) {
                                                        rateTextView.text = "Error de la API: ${response.code}"
                                                        lastUpdatedTextView.text = "Última actualización: --"
                                                    }
                                                }
                                            } catch (e: IOException) {
                                                withContext(Dispatchers.Main) {
                                                    rateTextView.text = "No hay internet: ${e.message}"
                                                    lastUpdatedTextView.text = "Última actualización: --"
                                                }
                                            }
                                        }
                                    }
}
