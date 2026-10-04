package com.hanati.bank.bankEx.loan.general.mapper;

import com.hanati.bank.bankEx.loan.general.domain.LoanRepaymentHistory;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface LoanRepaymentHistoryMapper {
    void insert(LoanRepaymentHistory history);
    List<LoanRepaymentHistory> findByApplicationId(Long applicationId);
    int countByApplicationId(Long applicationId);
}
