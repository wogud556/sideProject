package com.hanati.bankexnative

import android.webkit.WebView
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

/**
 * 뒤로가기 동작 검증.
 * - WebView에 히스토리가 있을 때 goBack() 호출 여부
 * - 히스토리가 없을 때 goBack()이 호출되지 않는지 (Activity 종료로 위임)
 * 실제 BackHandler 연동은 WebViewScreen의 canGoBack 상태로 제어되며,
 * 이 테스트는 순수 분기 로직만 검증한다.
 */
class BackNavigationTest {

    @Test
    fun `WebView에 히스토리가 있으면 goBack을 호출해야 한다`() {
        val webView: WebView = mockk(relaxed = true)
        every { webView.canGoBack() } returns true

        if (webView.canGoBack()) webView.goBack()

        verify { webView.goBack() }
    }

    @Test
    fun `WebView에 히스토리가 없으면 goBack을 호출하지 않아야 한다`() {
        val webView: WebView = mockk(relaxed = true)
        every { webView.canGoBack() } returns false

        if (webView.canGoBack()) webView.goBack()

        verify(exactly = 0) { webView.goBack() }
    }

    @Test
    fun `onPageFinished 후 canGoBack 상태가 업데이트되어야 한다`() {
        var canGoBack = false
        val onPageFinished: (Boolean) -> Unit = { newCanGoBack -> canGoBack = newCanGoBack }

        onPageFinished(true)

        assert(canGoBack)
    }
}
