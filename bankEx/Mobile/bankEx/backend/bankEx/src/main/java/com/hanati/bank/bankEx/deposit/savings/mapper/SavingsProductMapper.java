package com.hanati.bank.bankEx.deposit.savings.mapper;

import com.hanati.bank.bankEx.deposit.savings.domain.SavingsProduct;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface SavingsProductMapper {
    List<SavingsProduct> findAllOnSale();

    Optional<SavingsProduct> findById(String productId);

    void insert(SavingsProduct product);
}
