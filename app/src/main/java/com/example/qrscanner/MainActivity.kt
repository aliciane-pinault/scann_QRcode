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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QRScannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent()
                }
            }
        }
    }

    @Composable
    fun MainContent() {
        val scanLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
            val result = IntentIntegrator.parseActivityResult(result.resultCode, result.data)
            if (result != null) {
                if (result.contents == null) {
                    Toast.makeText(this@MainActivity, "Cancelled", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MainActivity, "Scanned: ${result.contents}", Toast.LENGTH_LONG).show()
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Title()
            Spacer(modifier = Modifier.height(16.dp))
            ScanButton { scanLauncher.launch(IntentIntegrator(this@MainActivity).createScanIntent()) }
        }
    }

    @Composable
    fun Title() {
        Text(
            text = "Titre",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = Color.Black
        )
    }

    @Composable
    fun ScanButton(onClick: () -> Unit) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .height(56.dp)
                .fillMaxWidth(fraction = 0.8f),
            colors = ButtonDefaults.buttonColors(Color.Blue)
        ) {
            Text(
                "Scan QR Code",
                color = Color.White,
                fontSize = 18.sp
            )
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        QRScannerTheme {
            MainContent()
        }
    }
}