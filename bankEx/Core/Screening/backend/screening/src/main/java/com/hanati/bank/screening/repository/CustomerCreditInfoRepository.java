package com.hanati.bank.screening.repository;

import com.hanati.bank.screening.entity.CustomerCreditInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CustomerCreditInfoRepository extends JpaRepository<CustomerCreditInfo, Long> {
    Optional<CustomerCreditInfo> findByUserId(String userId);
}
