package com.hanati.bank.bankEx.deposit.fixed.mapper;

import com.hanati.bank.bankEx.deposit.fixed.domain.FixedDepositAccount;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Mapper
public interface FixedDepositAccountMapper {
    Optional<FixedDepositAccount> findByDepositAccountId(String depositAccountId);

    List<FixedDepositAccount> findDueForMaturity(LocalDate today);

    void insert(FixedDepositAccount account);

    void update(FixedDepositAccount account);
}
