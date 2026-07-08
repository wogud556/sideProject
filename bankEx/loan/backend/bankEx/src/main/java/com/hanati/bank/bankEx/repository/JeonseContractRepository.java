package com.hanati.bank.bankEx.repository;

import com.hanati.bank.bankEx.entity.JeonseContract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JeonseContractRepository extends JpaRepository<JeonseContract, String> {
    Optional<JeonseContract> findByApplicationId(String applicationId);
}
