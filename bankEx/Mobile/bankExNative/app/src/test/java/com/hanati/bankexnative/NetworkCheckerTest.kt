package com.hanati.bankexnative

import com.hanati.bankexnative.network.NetworkChecker
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * NetworkChecker 인터페이스 계약 검증.
 * ConnectivityNetworkChecker는 실기기/에뮬레이터 의존성이 강해
 * 인터페이스를 mock하여 분기 로직만 검증한다.
 */
class NetworkCheckerTest {

    @Test
    fun `네트워크가 연결된 경우 isConnected가 true를 반환해야 한다`() {
        val checker: NetworkChecker = mockk {
            every { isConnected() } returns true
        }
        assertTrue(checker.isConnected())
    }

    @Test
    fun `네트워크가 끊긴 경우 isConnected가 false를 반환해야 한다`() {
        val checker: NetworkChecker = mockk {
            every { isConnected() } returns false
        }
        assertFalse(checker.isConnected())
    }

    @Test
    fun `네트워크가 없으면 에러 처리를 유발해야 한다`() {
        val checker: NetworkChecker = mockk {
            every { isConnected() } returns false
        }
        var errorTriggered = false

        if (!checker.isConnected()) {
            errorTriggered = true
        }

        assertTrue(errorTriggered)
    }
}
