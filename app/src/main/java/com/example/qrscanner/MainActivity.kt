package com.example.qrscanner

import android.content.Intent
import android.os.Build
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
                val jwtToken = result.contents.substringAfter("token jwt : ")
                // Envoie le JWT au serveur pour validation
                sendTokenToServer(jwtToken)
            }
        }
    }

    private fun getUnsafeOkHttpClient_1(): OkHttpClient {
        // Créez un TrustManager qui fait confiance au certificat CAs dans votre keystore
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val inputStream: InputStream =
            resources.openRawResource(R.raw.moncertificat) // le sertifiact du serveur
        val certificate = certificateFactory.generateCertificate(inputStream)
        inputStream.close()

        // Créez un keystore contenant notre certificat de confiance
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setCertificateEntry("ca", certificate)
        }

        // Utilisez-le pour construire un X509TrustManager.
        val trustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
                init(keyStore)
            }
        val trustManagers = trustManagerFactory.trustManagers
        check(trustManagers.size == 1 && trustManagers[0] is X509TrustManager) { "X509TrustManager non trouvé" }
        val trustManager = trustManagers[0] as X509TrustManager

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf<TrustManager>(trustManager), null)

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
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


    //passer outre les verif https
    private fun getUnsafeOkHttpClient(): OkHttpClient {
        try {
            // Créez un TrustManager qui ne fait pas de vérification des certificats
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(
                    chain: Array<java.security.cert.X509Certificate>,
                    authType: String
                ) {
                    // Ne fait rien, accepte tout
                }

                override fun checkServerTrusted(
                    chain: Array<java.security.cert.X509Certificate>,
                    authType: String
                ) {
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

    //envoyer le token sur le serveur sous le format json (dans le cas où on veut directement remplir une table avec les données par exemple)
    private fun sendTokenToServer_1(jwtToken: String) {
        val client = getUnsafeOkHttpClient()

        val jsonRequestBody = """
        {
            "token_jwt": "$jwtToken"
        }
        """.trimIndent()

        // Imprimez le corps de la requête JSON dans les logs pour le débogage
        Log.d("SendTokenToServer", "jsonRequestBody: $jsonRequestBody")

        // Définissez le MediaType pour "application/json"
        val mediaType = "application/json".toMediaTypeOrNull()
        val body = RequestBody.create(mediaType, jsonRequestBody)

        // Construction de la requête
        val request = Request.Builder()
            .url("https://192.168.200.23:443/api/tests")
            .post(body)
            .addHeader("content-type", "application/json")
            .build()

        // Envoi de la requête
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Error", e.localizedMessage)
                runOnUiThread {
                    Toast.makeText(
                        applicationContext,
                        "Erreur lors de la connexion au serveur : " + e,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        Toast.makeText(
                            applicationContext,
                            "données arriver sur le serveur sans problème $responseBody",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            applicationContext,
                            "données arriver sur le serveur mais soucis au traitement ",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        })
    }

    //requete post
    private fun sendTokenToServer(jwtToken: String) {
        val client = getUnsafeOkHttpClient()

        // Construction de l'URL avec le token JWT comme paramètre de requête
        val url = HttpUrl.Builder()
            .scheme("https")
            .host("192.168.200.23")
            .port(443)
            .addPathSegment("api")
            .addPathSegment("tests")
            .addQueryParameter("token_jwt", jwtToken) // Ajout du token JWT comme paramètre de requête
            .build()

        // Construction de la requête
        val request = Request.Builder()
            .url(url)
            .post(RequestBody.create(null, ByteArray(0))) // Corps vide pour une requête POST
            .addHeader("content-type", "application/json")
            .build()

        // Envoi de la requête
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Error", e.localizedMessage)
                // Gestion de l'échec de la requête
            }

            override fun onResponse(call: Call, response: Response) {
                // Gestion de la réponse
            }
        })
    }

}