package com.hanati.bankexnative.ui

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.hanati.bankexnative.config.WebConfig
import com.hanati.bankexnative.webview.AppWebChromeClient
import com.hanati.bankexnative.webview.AppWebViewClient
import com.hanati.bankexnative.webview.WebViewConfigurator

@Composable
fun WebViewScreen(url: String = WebConfig.BASE_URL) {
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // canGoBack이 true일 때만 BackHandler가 이벤트를 소비 — false면 시스템이 Activity 종료 처리
    BackHandler(enabled = canGoBack) {
        webViewRef?.goBack()
    }

    if (errorMessage != null) {
        ErrorScreen(
            message = errorMessage!!,
            onRetry = {
                errorMessage = null
                isLoading = true
                webViewRef?.loadUrl(url)
            }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                WebView(context).also { webView ->
                    webViewRef = webView
                    WebViewConfigurator().configure(webView.settings)
                    webView.webViewClient = AppWebViewClient(
                        onPageStarted = { isLoading = true },
                        onPageFinished = { newCanGoBack ->
                            isLoading = false
                            canGoBack = newCanGoBack
                        },
                        onError = { msg ->
                            isLoading = false
                            errorMessage = msg
                        }
                    )
                    webView.webChromeClient = AppWebChromeClient(
                        onProgressChanged = { progress ->
                            isLoading = progress < 100
                        }
                    )
                    webView.loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}
