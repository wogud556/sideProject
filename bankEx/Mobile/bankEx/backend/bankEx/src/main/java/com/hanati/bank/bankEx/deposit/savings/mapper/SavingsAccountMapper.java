package com.hanati.bank.bankEx.deposit.savings.mapper;

import com.hanati.bank.bankEx.deposit.savings.domain.SavingsAccount;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Mapper
public interface SavingsAccountMapper {
    Optional<SavingsAccount> findByAccountNo(String accountNo);

    List<SavingsAccount> findActiveByUserIdAndProductId(String userId, String productId);

    List<SavingsAccount> findDueForTransfer(int transferDay);

    List<SavingsAccount> findDueForMaturity(LocalDate today);

    void insert(SavingsAccount account);

    void update(SavingsAccount account);
}
