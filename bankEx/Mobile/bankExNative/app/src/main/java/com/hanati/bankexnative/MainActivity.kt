package com.hanati.bankexnative

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hanati.bankexnative.ui.WebViewScreen
import com.hanati.bankexnative.ui.theme.BankExNativeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BankExNativeTheme {
                WebViewScreen()
            }
        }
    }
}