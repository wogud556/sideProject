package com.hanati.bank.bankEx.login.service;

import com.hanati.bank.bankEx.common.exception.BusinessException;
import com.hanati.bank.bankEx.common.exception.ErrorCode;
import com.hanati.bank.bankEx.deposit.general.entity.AccountInfo;
import com.hanati.bank.bankEx.deposit.general.repository.AccountInfoRepository;
import com.hanati.bank.bankEx.login.dto.LoginRequest;
import com.hanati.bank.bankEx.login.dto.LoginResponse;
import com.hanati.bank.bankEx.login.dto.SignupRequest;
import com.hanati.bank.bankEx.login.dto.SignupResponse;
import com.hanati.bank.bankEx.login.dto.UserProfileResponse;
import com.hanati.bank.bankEx.login.entity.UserInfo;
import com.hanati.bank.bankEx.login.repository.UserInfoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * loginService에 대한 순수 Mockito 단위 테스트 (Spring context, DB 불필요).
 */
@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private UserInfoRepository userInfoRepository;
    @Mock
    private AccountInfoRepository accountInfoRepository;

    @InjectMocks
    private loginService loginService;

    // LoginRequest는 @RequestBody(Jackson)로만 생성되도록 설계되어 있어 all-args 생성자/세터가 없다.
    // 테스트에서 직접 값을 채우기 위해 ReflectionTestUtils로 필드를 주입한다.
    private LoginRequest loginRequest(String userId, String password) {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "userId", userId);
        ReflectionTestUtils.setField(request, "password", password);
        return request;
    }

    private UserInfo user(String userId, String password) {
        return UserInfo.builder()
                .userId(userId)
                .password(password)
                .userName("홍길동")
                .phone("01012345678")
                .customerStatus("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ---------- login ----------

    @Test
    void login_success_returnsLoginResponse() {
        when(userInfoRepository.findById("user1")).thenReturn(Optional.of(user("user1", "pw1234")));

        LoginResponse response = loginService.login(loginRequest("user1", "pw1234"));

        assertEquals("user1", response.getUserId());
        assertEquals("홍길동", response.getUserName());
        assertEquals("로그인 성공", response.getMessage());
    }

    @Test
    void login_userNotFound_throwsIllegalArgumentException() {
        when(userInfoRepository.findById("noSuchUser")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> loginService.login(loginRequest("noSuchUser", "pw1234")));
    }

    @Test
    void login_wrongPassword_throwsIllegalArgumentException() {
        when(userInfoRepository.findById("user1")).thenReturn(Optional.of(user("user1", "pw1234")));

        assertThrows(IllegalArgumentException.class,
                () -> loginService.login(loginRequest("user1", "wrongPw")));
    }

    // ---------- signup ----------

    @Test
    void signup_success_savesUserAndAccountAndReturnsResponse() {
        when(userInfoRepository.existsById("newUser")).thenReturn(false);

        SignupResponse response = loginService.signup(
                new SignupRequest("newUser", "pw1234", "홍길동", "01012345678", "1234"));

        ArgumentCaptor<UserInfo> userCaptor = ArgumentCaptor.forClass(UserInfo.class);
        verify(userInfoRepository).save(userCaptor.capture());
        assertEquals("newUser", userCaptor.getValue().getUserId());
        assertEquals("ACTIVE", userCaptor.getValue().getCustomerStatus());

        ArgumentCaptor<AccountInfo> accountCaptor = ArgumentCaptor.forClass(AccountInfo.class);
        verify(accountInfoRepository).save(accountCaptor.capture());
        AccountInfo savedAccount = accountCaptor.getValue();
        assertEquals("newUser", savedAccount.getUserId());
        assertEquals("D001", savedAccount.getProductCode());
        assertEquals(0L, savedAccount.getBalance());
        assertEquals("ACTIVE", savedAccount.getAccountStatus());
        assertEquals("1234", savedAccount.getAccountPassword());

        assertEquals("newUser", response.getUserId());
        assertEquals("홍길동", response.getUserName());
        assertEquals(savedAccount.getAccountNumber(), response.getAccountNumber());
        assertEquals("회원가입이 완료되었습니다", response.getMessage());
    }

    @Test
    void signup_duplicateUserId_throwsIllegalArgumentException() {
        when(userInfoRepository.existsById("existingUser")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> loginService.signup(new SignupRequest("existingUser", "pw1234", "홍길동", "01012345678", "1234")));

        verify(userInfoRepository, never()).save(any());
        verify(accountInfoRepository, never()).save(any());
    }

    @Test
    void signup_nullAccountPassword_throwsBusinessExceptionInvalidRequest() {
        when(userInfoRepository.existsById("newUser")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loginService.signup(new SignupRequest("newUser", "pw1234", "홍길동", "01012345678", null)));

        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
        verify(userInfoRepository, never()).save(any());
        verify(accountInfoRepository, never()).save(any());
    }

    @Test
    void signup_blankAccountPassword_throwsBusinessExceptionInvalidRequest() {
        when(userInfoRepository.existsById("newUser")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loginService.signup(new SignupRequest("newUser", "pw1234", "홍길동", "01012345678", "   ")));

        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
        verify(userInfoRepository, never()).save(any());
    }

    // ---------- getProfile ----------

    @Test
    void getProfile_success_returnsUserProfileWithAccounts() {
        when(userInfoRepository.findById("user1")).thenReturn(Optional.of(user("user1", "pw1234")));
        AccountInfo account = AccountInfo.builder()
                .accountId(1L)
                .userId("user1")
                .accountNumber("111-222-33")
                .balance(10_000L)
                .accountStatus("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();
        when(accountInfoRepository.findByUserId("user1")).thenReturn(List.of(account));

        UserProfileResponse response = loginService.getProfile("user1");

        assertEquals("user1", response.getUserId());
        assertEquals("홍길동", response.getUserName());
        assertEquals(1, response.getAccounts().size());
        assertEquals("111-222-33", response.getAccounts().get(0).getAccountNumber());
        assertEquals(10_000L, response.getAccounts().get(0).getBalance());
    }

    @Test
    void getProfile_userNotFound_throwsIllegalArgumentException() {
        when(userInfoRepository.findById("noSuchUser")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> loginService.getProfile("noSuchUser"));
    }
}
