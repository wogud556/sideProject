package com.hanati.bank.bankEx.deposit.savings.mapper;

import com.hanati.bank.bankEx.deposit.savings.domain.AutoTransfer;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface AutoTransferMapper {
    Optional<AutoTransfer> findByAccountNo(String accountNo);

    void insert(AutoTransfer autoTransfer);
}
