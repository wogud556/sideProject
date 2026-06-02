package com.hanati.bank.bankEx.repository;

import com.hanati.bank.bankEx.entity.AccountInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountInfoRepository extends JpaRepository<AccountInfo, Long> {
    List<AccountInfo> findByUserId(String userId);
}
