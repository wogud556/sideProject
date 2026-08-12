package com.hanati.bank.refinance.refinance.mapper;

import com.hanati.bank.refinance.refinance.dto.ErrorSearchResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 실패거래 조회 (명세 19번) — 신청번호/고객번호 조인, 다중 조건 동적 검색이 필요해 MyBatis를 사용한다.
 * (단순 CRUD 성격의 나머지 조회는 JPA Repository를 사용 — bankEx의 Dual ORM 컨벤션과 동일)
 */
@Mapper
public interface RefinanceErrorMapper {

    List<ErrorSearchResult> search(@Param("transactionDate") LocalDate transactionDate,
                                    @Param("applicationNo") String applicationNo,
                                    @Param("customerId") Long customerId,
                                    @Param("failedStep") String failedStep,
                                    @Param("errorCode") String errorCode,
                                    @Param("status") String status);
}
