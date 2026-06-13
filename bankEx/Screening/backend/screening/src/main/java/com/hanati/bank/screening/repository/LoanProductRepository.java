package com.hanati.bank.screening.repository;

import com.hanati.bank.screening.entity.LoanProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoanProductRepository extends JpaRepository<LoanProduct, Long> {
    List<LoanProduct> findByUseYn(String useYn);
}
