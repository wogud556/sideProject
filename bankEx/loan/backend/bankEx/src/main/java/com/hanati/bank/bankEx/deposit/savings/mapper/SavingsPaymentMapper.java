package com.hanati.bank.bankEx.deposit.savings.mapper;

import com.hanati.bank.bankEx.deposit.savings.domain.SavingsPaymentHistory;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SavingsPaymentMapper {
    List<SavingsPaymentHistory> findByAccountNo(String accountNo);

    int countSuccessByAccountNoAndSeq(String accountNo, int paymentSeq);

    void insert(SavingsPaymentHistory payment);
}
