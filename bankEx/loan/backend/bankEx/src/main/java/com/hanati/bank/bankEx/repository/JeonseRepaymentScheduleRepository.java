package com.hanati.bank.bankEx.repository;

import com.hanati.bank.bankEx.entity.JeonseRepaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JeonseRepaymentScheduleRepository extends JpaRepository<JeonseRepaymentSchedule, Long> {
    List<JeonseRepaymentSchedule> findByApplicationIdOrderByPaymentSeq(String applicationId);
}
