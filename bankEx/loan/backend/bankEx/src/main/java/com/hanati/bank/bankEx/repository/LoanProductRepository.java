package com.hanati.bank.bankEx.repository;

import com.hanati.bank.bankEx.entity.LoanProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanProductRepository extends JpaRepository<LoanProduct, Long> {
}
