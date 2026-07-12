package com.hanati.bank.bankEx.loan.jeonse.mapper;

import com.hanati.bank.bankEx.loan.jeonse.domain.JeonseLoanProduct;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface JeonseLoanProductMapper {
    List<JeonseLoanProduct> findAllActive();

    Optional<JeonseLoanProduct> findById(String productId);

    long count();

    void insert(JeonseLoanProduct product);
}
