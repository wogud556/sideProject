package com.hanati.bankexnative.webview

import android.webkit.WebSettings
import com.hanati.bankexnative.config.WebConfig

class WebViewConfigurator {
    fun configure(settings: WebSettings) {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.userAgentString = WebConfig.USER_AGENT
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
    }
}
