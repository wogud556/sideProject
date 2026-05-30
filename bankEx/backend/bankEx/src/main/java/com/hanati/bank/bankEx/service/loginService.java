package com.hanati.bank.bankEx.service;

import com.hanati.bank.bankEx.dto.LoginRequest;
import com.hanati.bank.bankEx.dto.LoginResponse;
import org.springframework.stereotype.Service;

@Service
public class loginService {

    // 임시 하드코딩 계정 (추후 DB 조회로 교체)
    private static final String TEMP_USER_ID = "admin";
    private static final String TEMP_PASSWORD = "1234";
    private static final String TEMP_USER_NAME = "홍길동";

    public LoginResponse login(LoginRequest request) {
        if (!TEMP_USER_ID.equals(request.getUserId()) ||
            !TEMP_PASSWORD.equals(request.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 틀렸습니다");
        }
        return new LoginResponse(TEMP_USER_ID, TEMP_USER_NAME, "로그인 성공");
    }
}
