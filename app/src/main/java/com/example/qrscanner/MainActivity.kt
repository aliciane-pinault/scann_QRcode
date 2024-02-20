package com.example.qrscanner

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.zxing.integration.android.IntentIntegrator
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.IOException
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.CertificateFactory
import java.security.KeyStore
import javax.net.ssl.SSLContext
import java.io.InputStream
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.TrustManagerFactory

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
    private fun getUnsafeOkHttpClient_1(): OkHttpClient {
        // Créez un TrustManager qui fait confiance au certificat CAs dans votre keystore
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val inputStream: InputStream = resources.openRawResource(R.raw.moncertificat) // le sertifiact du serveur
        val certificate = certificateFactory.generateCertificate(inputStream)
        inputStream.close()

        // Créez un keystore contenant notre certificat de confiance
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setCertificateEntry("ca", certificate)
        }

        // Utilisez-le pour construire un X509TrustManager.
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore)
        }
        val trustManagers = trustManagerFactory.trustManagers
        check(trustManagers.size == 1 && trustManagers[0] is X509TrustManager) { "X509TrustManager non trouvé" }
        val trustManager = trustManagers[0] as X509TrustManager

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf<TrustManager>(trustManager), null)

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .readTimeout(30, TimeUnit.SECONDS) // Augmente le timeout de lecture à 30 secondes
            .writeTimeout(30, TimeUnit.SECONDS) // Augmente le timeout d'écriture à 30 secondes
            .connectTimeout(30, TimeUnit.SECONDS) // Augmente le timeout de connexion à 30 secondes
            .build()
    }


    private fun getUnsafeOkHttpClient_2(): OkHttpClient {
        try {
            // Créez un gestionnaire de confiance qui ne valide pas les chaînes de certificats
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            // Installez le gestionnaire de confiance tout en gardant le SSLContext vide
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            // Créez un OkHttpClient et installez le socketFactory avec le sslContext
            return OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun getUnsafeOkHttpClient_3(): OkHttpClient {
        try {
            // Créez un gestionnaire de confiance qui ne valide pas les chaînes de certificats
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            // Installez le gestionnaire de confiance tout en gardant le SSLContext vide
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            // Créez un OkHttpClient et installez le socketFactory avec le sslContext
            val builder = OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                // Ajoutez le HostnameVerifier personnalisé ici
                .hostnameVerifier { _, _ -> true }

            return builder.build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }




    //passer outre les verif https
    private fun getUnsafeOkHttpClient(): OkHttpClient {
        try {
            // Créez un TrustManager qui ne fait pas de vérification des certificats
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
                    // Ne fait rien, accepte tout
                }

                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
                    // Ne fait rien, accepte tout
                }

                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
                    return arrayOf() // Tableau vide
                }
            })

            // Installez le TrustManager tout en gardant le SSLContext vide
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            // Créez un OkHttpClient et installez le socketFactory avec le sslContext
            val builder = OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true } // Accepte n'importe quel nom d'hôte

            return builder.build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }




    //envoyer le token du QRcode vers le serveur en format json
    private fun sendTokenToServer_1(jwtToken: String) {
        val client = getUnsafeOkHttpClient() // Utilisez le client configuré
        val mediaType = "application/json".toMediaTypeOrNull()
        val body = RequestBody.create(mediaType, "{\"token\":\"$jwtToken\"}")
        val request = Request.Builder()
            .url("https://autopresence.isen.fr/api/tests")
            .post(body)
            .addHeader("content-type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Gestion de l'échec de la requête (n'arrive pas a envoyer la requete au serveur)
                Log.e("Error",e.localizedMessage)
                runOnUiThread {
                    Toast.makeText(applicationContext, "Erreur lors de la connexion au serveur : " + e, Toast.LENGTH_LONG).show()
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

    //envoyer des donnees sur le serveur sous forme de de text
    private fun sendTokenToServer(qrContent: String) {
        val client = getUnsafeOkHttpClient() // Utilisez le client configuré pour passer outre les vérifications SSL
        // Définissez le MediaType pour "text/plain" au lieu de "application/json"
        val mediaType = "text/plain".toMediaTypeOrNull()
        // Créez un RequestBody avec le contenu QR en format texte
        val body = RequestBody.create(mediaType, qrContent)
        // Construisez la requête en précisant le type de contenu "text/plain"
        val request = Request.Builder()
            .url("https://autopresence.isen.fr/api/tests")
            .post(body)
            .addHeader("content-type", "text/plain")
            .build()

        // Effectuez l'appel réseau de manière asynchrone
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Gestion de l'échec de la requête
                Log.e("Error", e.localizedMessage)
                runOnUiThread {
                    Toast.makeText(applicationContext, "Erreur lors de la connexion au serveur : " + e, Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        // Afficher une notification pour une réponse réussie
                        Toast.makeText(applicationContext, "Réponse du serveur : $responseBody", Toast.LENGTH_LONG).show()
                    } else {
                        // Gestion de la réponse d'erreur
                        Toast.makeText(applicationContext, "Erreur de réponse du serveur", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}