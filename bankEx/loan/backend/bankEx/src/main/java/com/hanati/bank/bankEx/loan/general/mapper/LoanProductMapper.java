package com.hanati.bank.bankEx.loan.general.mapper;

import com.hanati.bank.bankEx.loan.general.domain.LoanProduct;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface LoanProductMapper {
    List<LoanProduct> findAll();

    Optional<LoanProduct> findById(Long productId);

    long count();

    void insert(LoanProduct product);
}
