package com.hanati.bank.bankEx.loan.jeonse.mapper;

import com.hanati.bank.bankEx.loan.jeonse.domain.JeonseContract;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface JeonseContractMapper {
    Optional<JeonseContract> findByApplicationId(String applicationId);

    int countById(String contractId);

    default boolean existsById(String contractId) {
        return countById(contractId) > 0;
    }

    void insert(JeonseContract contract);
}
