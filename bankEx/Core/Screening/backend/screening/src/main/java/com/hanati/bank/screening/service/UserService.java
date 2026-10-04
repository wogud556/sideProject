package com.hanati.bank.screening.service;

import com.hanati.bank.screening.dto.*;
import com.hanati.bank.screening.entity.UserInfo;
import com.hanati.bank.screening.repository.CustomerCreditInfoRepository;
import com.hanati.bank.screening.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserInfoRepository userInfoRepository;
    private final CustomerCreditInfoRepository creditInfoRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public void signup(SignupRequest req) {
        if (userInfoRepository.existsById(req.getUserId())) {
            throw new IllegalArgumentException("이미 사용중인 아이디입니다.");
        }
        UserInfo user = UserInfo.builder()
                .userId(req.getUserId())
                .password(passwordEncoder.encode(req.getPassword()))
                .userName(req.getUserName())
                .birthDate(req.getBirthDate())
                .phoneNumber(req.getPhoneNumber())
                .build();
        userInfoRepository.save(user);
    }

    public LoginResponse login(LoginRequest req) {
        UserInfo user = userInfoRepository.findById(req.getUserId()).orElse(null);
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호를 확인해 주세요.");
        }
        return new LoginResponse(user.getUserId(), user.getUserName(), "로그인 성공");
    }

    public UserProfileResponse getProfile(String userId) {
        UserInfo user = userInfoRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        var credit = creditInfoRepository.findByUserId(userId).orElse(null);
        return new UserProfileResponse(user, credit);
    }
}
