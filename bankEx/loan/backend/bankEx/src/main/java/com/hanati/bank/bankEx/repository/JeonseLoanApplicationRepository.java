package com.hanati.bank.bankEx.repository;

import com.hanati.bank.bankEx.entity.JeonseLoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JeonseLoanApplicationRepository extends JpaRepository<JeonseLoanApplication, String> {
    List<JeonseLoanApplication> findByUserId(String userId);
}
