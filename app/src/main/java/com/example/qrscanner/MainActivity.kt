package com.example.qrscanner

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.zxing.integration.android.IntentIntegrator
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnScan = findViewById<Button>(R.id.btnScan)
        btnScan.setOnClickListener {
            // Lancement du scanner QR
            IntentIntegrator(this).initiateScan()
        }
    }

    // Cette méthode traite le résultat du scan
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Scanned: ${result.contents}", Toast.LENGTH_LONG).show()
                // Envoie le JWT au serveur pour validation
                sendTokenToServer(result.contents)
            }
        }
    }

    private fun sendTokenToServer(jwtToken: String) {
        val client = OkHttpClient()
        val mediaType = "application/json".toMediaTypeOrNull()
        val body = RequestBody.create(mediaType, "{\"token\":\"$jwtToken\"}")
        val request = Request.Builder()
            .url("http://votre_serveur/validate-jwt") // TODO : Remplacez l'URL de notre serveur
            .post(body)
            .addHeader("content-type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Gestion de l'échec de la requête (n'arrive pas a envoyer la requete au serveur)
                runOnUiThread {
                    Toast.makeText(applicationContext, "Erreur lors de la connexion au serveur", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        // afficher une notification pour un JWT valide
                        Toast.makeText(applicationContext, "JWT Valide : $responseBody", Toast.LENGTH_LONG).show()
                    } else {
                        // Gestion de la réponse d'erreur pour un token JWt invalide (pas reconnue pas la bonne heure ...)
                        Toast.makeText(applicationContext, "JWT Invalide", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}