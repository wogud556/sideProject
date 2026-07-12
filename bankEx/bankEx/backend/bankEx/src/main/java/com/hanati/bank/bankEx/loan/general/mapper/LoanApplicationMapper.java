package com.hanati.bank.bankEx.loan.general.mapper;

import com.hanati.bank.bankEx.loan.general.domain.LoanApplication;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface LoanApplicationMapper {
    Optional<LoanApplication> findById(Long applicationId);

    List<LoanApplication> findByUserId(String userId);

    void insert(LoanApplication application);

    void update(LoanApplication application);
}
