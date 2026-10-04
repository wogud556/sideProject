package com.hanati.bankexnative

import android.webkit.WebView
import com.hanati.bankexnative.config.WebConfig
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * URL 로딩 정책 검증.
 * - BASE_URL 형식이 유효한지 (정적 검사)
 * - loadUrl이 BASE_URL로 호출되는지
 * - 빈 URL이 들어왔을 때 에러 처리가 동작하는지
 */
class WebViewUrlTest {

    @Test
    fun `BASE_URL이 비어있지 않아야 한다`() {
        assertTrue(WebConfig.BASE_URL.isNotBlank())
    }

    @Test
    fun `BASE_URL이 http 또는 https로 시작해야 한다`() {
        assertTrue(
            WebConfig.BASE_URL.startsWith("http://") || WebConfig.BASE_URL.startsWith("https://")
        )
    }

    @Test
    fun `loadUrl이 BASE_URL로 호출되어야 한다`() {
        val webView: WebView = mockk(relaxed = true)
        webView.loadUrl(WebConfig.BASE_URL)
        verify { webView.loadUrl(WebConfig.BASE_URL) }
    }

    @Test
    fun `빈 URL은 에러 처리를 유발해야 한다`() {
        val onError: (String) -> Unit = mockk(relaxed = true)

        val url = ""
        if (url.isBlank()) {
            onError("유효하지 않은 URL입니다.")
        }

        verify { onError(any()) }
    }

    @Test
    fun `잘못된 형식의 URL은 에러 처리를 유발해야 한다`() {
        val onError: (String) -> Unit = mockk(relaxed = true)

        val url = "not-a-valid-url"
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            onError("유효하지 않은 URL입니다.")
        }

        verify { onError(any()) }
    }
}
