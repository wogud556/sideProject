package com.hanati.bank.bankEx.loan.jeonse.mapper;

import com.hanati.bank.bankEx.loan.jeonse.domain.JeonseLoanApplication;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface JeonseLoanApplicationMapper {
    Optional<JeonseLoanApplication> findById(String applicationId);

    int countById(String applicationId);

    default boolean existsById(String applicationId) {
        return countById(applicationId) > 0;
    }

    void insert(JeonseLoanApplication application);

    void update(JeonseLoanApplication application);
}
