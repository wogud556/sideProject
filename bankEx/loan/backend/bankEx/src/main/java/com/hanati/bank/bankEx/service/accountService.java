package com.hanati.bank.bankEx.service;

import com.hanati.bank.bankEx.dto.AccountResponse;
import com.hanati.bank.bankEx.repository.AccountInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class accountService {

    private final AccountInfoRepository accountInfoRepository;

    public List<AccountResponse> getAccounts(String userId) {
        return accountInfoRepository.findByUserId(userId).stream()
                .map(a -> new AccountResponse(
                        a.getAccountId(),
                        a.getAccountNumber(),
                        a.getBalance(),
                        a.getCreatedAt().toString()
                ))
                .collect(Collectors.toList());
    }
}
