package com.hanati.bank.bankEx.login.service;

import com.hanati.bank.bankEx.deposit.general.dto.AccountResponse;
import com.hanati.bank.bankEx.deposit.general.entity.AccountInfo;
import com.hanati.bank.bankEx.deposit.general.repository.AccountInfoRepository;
import com.hanati.bank.bankEx.login.dto.LoginRequest;
import com.hanati.bank.bankEx.login.dto.LoginResponse;
import com.hanati.bank.bankEx.login.dto.SignupRequest;
import com.hanati.bank.bankEx.login.dto.SignupResponse;
import com.hanati.bank.bankEx.login.dto.UserProfileResponse;
import com.hanati.bank.bankEx.login.entity.UserInfo;
import com.hanati.bank.bankEx.login.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class loginService {

    private final UserInfoRepository userInfoRepository;
    private final AccountInfoRepository accountInfoRepository;

    public LoginResponse login(LoginRequest request) {
        UserInfo user = userInfoRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 틀렸습니다"));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 틀렸습니다");
        }

        return new LoginResponse(user.getUserId(), user.getUserName(), "로그인 성공");
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userInfoRepository.existsById(request.getUserId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다");
        }

        UserInfo user = UserInfo.builder()
                .userId(request.getUserId())
                .password(request.getPassword())
                .userName(request.getUserName())
                .phone(request.getPhone())
                .customerStatus("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();
        userInfoRepository.save(user);

        String accountNumber = generateAccountNumber();
        AccountInfo account = AccountInfo.builder()
                .userId(request.getUserId())
                .accountNumber(accountNumber)
                .productCode("D001")
                .accountName("입출금통장")
                .balance(0L)
                .accountStatus("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();
        accountInfoRepository.save(account);

        return new SignupResponse(user.getUserId(), user.getUserName(), accountNumber, "회원가입이 완료되었습니다");
    }

    public UserProfileResponse getProfile(String userId) {
        UserInfo user = userInfoRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        List<AccountResponse> accounts = accountInfoRepository.findByUserId(userId).stream()
                .map(a -> new AccountResponse(
                        a.getAccountId(),
                        a.getAccountNumber(),
                        a.getBalance(),
                        a.getCreatedAt().toString()
                ))
                .collect(Collectors.toList());

        return new UserProfileResponse(
                user.getUserId(),
                user.getUserName(),
                user.getPhone(),
                user.getCreatedAt().toString(),
                accounts
        );
    }

    private String generateAccountNumber() {
        Random random = new Random();
        String prefix = String.format("%03d", random.nextInt(1000));
        String middle = String.format("%06d", random.nextInt(1000000));
        String suffix = String.format("%02d", random.nextInt(100));
        return prefix + "-" + middle + "-" + suffix;
    }
}
