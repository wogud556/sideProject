package com.hanati.bank.mortgage.application.repository;

import com.hanati.bank.mortgage.application.entity.MortgageApplication;
import com.hanati.bank.mortgage.application.enums.ApplicationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MortgageApplicationRepository extends JpaRepository<MortgageApplication, Long> {

    List<MortgageApplication> findAllByOrderByApplicationIdDesc();

    List<MortgageApplication> findByStatusOrderByApplicationIdAsc(ApplicationStatus status);

    List<MortgageApplication> findByCustomerIdOrderByApplicationIdDesc(String customerId);

    /** 상태를 바꾸는 모든 처리는 이 잠금을 먼저 잡는다. 같은 건을 두 담당자가 동시에 처리하는 경합을 막는다 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from MortgageApplication a where a.applicationId = :id")
    Optional<MortgageApplication> findByIdForUpdate(@Param("id") Long id);
}
