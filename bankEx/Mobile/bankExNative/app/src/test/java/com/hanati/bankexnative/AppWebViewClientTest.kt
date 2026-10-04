package com.hanati.bankexnative

import android.content.Context
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import com.hanati.bankexnative.webview.AppWebViewClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * AppWebViewClient의 에러 처리 및 페이지 로딩 콜백이 올바르게 동작하는지 검증.
 * - 메인 프레임 에러만 onError를 호출하는지
 * - SSL 에러 시 handler.cancel()이 호출되는지
 * - 서브 프레임 에러는 무시하는지
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AppWebViewClientTest {

    private val onPageStarted: () -> Unit = mockk(relaxed = true)
    private val onPageFinished: (Boolean) -> Unit = mockk(relaxed = true)
    private val onError: (String) -> Unit = mockk(relaxed = true)

    private lateinit var client: AppWebViewClient
    private lateinit var webView: WebView

    @Before
    fun setUp() {
        client = AppWebViewClient(onPageStarted, onPageFinished, onError)
        val context = ApplicationProvider.getApplicationContext<Context>()
        webView = WebView(context)
    }

    @Test
    fun `onPageStarted가 콜백을 호출해야 한다`() {
        client.onPageStarted(webView, "http://10.0.2.2:5173", null)
        verify { onPageStarted() }
    }

    @Test
    fun `onPageFinished가 콜백을 호출해야 한다`() {
        client.onPageFinished(webView, "http://10.0.2.2:5173")
        verify { onPageFinished(any()) }
    }

    @Test
    fun `메인 프레임 에러 발생 시 onError를 호출해야 한다`() {
        val request: WebResourceRequest = mockk {
            every { isForMainFrame } returns true
        }
        val error: WebResourceError = mockk {
            every { errorCode } returns -2
            every { description } returns "net::ERR_NAME_NOT_RESOLVED"
        }

        client.onReceivedError(webView, request, error)

        verify { onError(any()) }
    }

    @Test
    fun `서브 프레임 에러 발생 시 onError를 호출하지 않아야 한다`() {
        val request: WebResourceRequest = mockk {
            every { isForMainFrame } returns false
        }
        val error: WebResourceError = mockk {
            every { errorCode } returns -2
            every { description } returns "error"
        }

        client.onReceivedError(webView, request, error)

        verify(exactly = 0) { onError(any()) }
    }

    @Test
    fun `HTTP 에러 발생 시 onError를 호출해야 한다`() {
        val request: WebResourceRequest = mockk {
            every { isForMainFrame } returns true
        }
        val errorResponse: WebResourceResponse = mockk {
            every { statusCode } returns 500
        }

        client.onReceivedHttpError(webView, request, errorResponse)

        verify { onError(any()) }
    }

    @Test
    fun `SSL 에러 발생 시 handler를 cancel하고 onError를 호출해야 한다`() {
        val handler: SslErrorHandler = mockk(relaxed = true)
        val error: SslError = mockk(relaxed = true)

        client.onReceivedSslError(webView, handler, error)

        verify { handler.cancel() }
        verify { onError(any()) }
    }
}
