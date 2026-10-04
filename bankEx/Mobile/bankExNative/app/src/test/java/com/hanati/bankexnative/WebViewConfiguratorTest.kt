package com.hanati.bankexnative

import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import com.hanati.bankexnative.config.WebConfig
import com.hanati.bankexnative.webview.WebViewConfigurator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * WebViewConfigurator가 WebSettings에 올바른 값을 설정하는지 검증.
 * Robolectric으로 실제 WebView 인스턴스를 생성해 설정값을 직접 확인한다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WebViewConfiguratorTest {

    private fun createConfiguredWebView(): WebView {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val webView = WebView(context)
        WebViewConfigurator().configure(webView.settings)
        return webView
    }

    @Test
    fun `JavaScript가 활성화되어야 한다`() {
        val webView = createConfiguredWebView()
        assertTrue(webView.settings.javaScriptEnabled)
    }

    @Test
    fun `DOM Storage가 활성화되어야 한다`() {
        val webView = createConfiguredWebView()
        assertTrue(webView.settings.domStorageEnabled)
    }

    @Test
    fun `UserAgent가 WebConfig 상수값으로 설정되어야 한다`() {
        val webView = createConfiguredWebView()
        assertEquals(WebConfig.USER_AGENT, webView.settings.userAgentString)
    }

    @Test
    fun `MixedContentMode가 COMPATIBILITY_MODE로 설정되어야 한다`() {
        val webView = createConfiguredWebView()
        assertEquals(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE, webView.settings.mixedContentMode)
    }

    @Test
    fun `useWideViewPort가 활성화되어야 한다`() {
        val webView = createConfiguredWebView()
        assertTrue(webView.settings.useWideViewPort)
    }
}
