package com.hanati.bank.bankEx.deposit.fixed.mapper;

import com.hanati.bank.bankEx.deposit.fixed.domain.FixedDepositProduct;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface FixedDepositProductMapper {
    List<FixedDepositProduct> findAllOnSale();

    Optional<FixedDepositProduct> findById(String depositProductId);

    void insert(FixedDepositProduct product);
}
