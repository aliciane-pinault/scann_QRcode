package com.example.qrscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.qrscanner.ui.theme.QRScannerTheme
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import com.google.zxing.integration.android.IntentIntegrator
import android.widget.Toast


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QRScannerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Replace Greeting call with QRCodeScannerButton
                    QRCodeScannerButton()
                }
            }
        }
    }

    @Composable
    fun QRCodeScannerButton() {
        // Prepare launcher for result
        val scanLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
                val intentResult =
                    IntentIntegrator.parseActivityResult(result.resultCode, result.data)
                if (intentResult != null) {
                    if (intentResult.contents == null) {
                        Toast.makeText(applicationContext, "Cancelled", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(
                            applicationContext,
                            "Scanned: ${intentResult.contents}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

        Button(onClick = {
            val integrator = IntentIntegrator(this@MainActivity)
            // integrator.captureActivity = CaptureActivity::class.java
            scanLauncher.launch(integrator.createScanIntent())
        }) {
            Text("Scan QR Code")
        }
    }
}
