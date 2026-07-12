package com.hanati.bank.bankEx.loan.jeonse.mapper;

import com.hanati.bank.bankEx.loan.jeonse.domain.JeonseRepaymentSchedule;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface JeonseRepaymentScheduleMapper {
    List<JeonseRepaymentSchedule> findByApplicationIdOrderByPaymentSeq(String applicationId);

    void insert(JeonseRepaymentSchedule schedule);
}
