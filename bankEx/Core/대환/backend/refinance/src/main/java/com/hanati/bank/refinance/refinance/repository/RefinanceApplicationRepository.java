package com.hanati.bank.refinance.refinance.repository;

import com.hanati.bank.refinance.refinance.domain.RefinanceStatus;
import com.hanati.bank.refinance.refinance.entity.RefinanceApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RefinanceApplicationRepository extends JpaRepository<RefinanceApplication, Long> {

    boolean existsByApplicationNo(String applicationNo);

    Optional<RefinanceApplication> findByApplicationNo(String applicationNo);

    List<RefinanceApplication> findByCustomerIdAndStatusIn(Long customerId, Collection<RefinanceStatus> statuses);

    List<RefinanceApplication> findAllByOrderByApplicationDateDesc();

    /**
     * 상태조건부 업데이트. WHERE 절에 현재 상태를 함께 걸어, 동시에 두 요청이 같은 신청 건을
     * 실행/승인/재처리하려 할 때 먼저 커밋된 하나만 반영되도록 한다 (명세 22번).
     * 영향받은 row 수가 0이면 호출 측에서 CONCURRENT_MODIFICATION으로 처리한다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefinanceApplication a SET a.status = :to WHERE a.applicationId = :id AND a.status = :from")
    int updateStatusIfMatch(@Param("id") Long applicationId,
                             @Param("from") RefinanceStatus from,
                             @Param("to") RefinanceStatus to);
}
