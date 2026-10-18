package com.hanati.bank.mortgage.collateral.repository;

import com.hanati.bank.mortgage.collateral.entity.MortgageLien;
import com.hanati.bank.mortgage.collateral.enums.LienType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MortgageLienRepository extends JpaRepository<MortgageLien, Long> {

    List<MortgageLien> findByPropertyIdOrderByLienRankAsc(Long propertyId);

    void deleteByPropertyIdAndLienTypeIn(Long propertyId, List<LienType> lienTypes);
}
