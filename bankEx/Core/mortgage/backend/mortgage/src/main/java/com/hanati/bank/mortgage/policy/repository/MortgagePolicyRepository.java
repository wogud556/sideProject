package com.hanati.bank.mortgage.policy.repository;

import com.hanati.bank.mortgage.policy.entity.MortgagePolicy;
import com.hanati.bank.mortgage.policy.enums.PolicyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MortgagePolicyRepository extends JpaRepository<MortgagePolicy, Long> {

    List<MortgagePolicy> findByPolicyType(PolicyType policyType);

    List<MortgagePolicy> findAllByOrderByPolicyTypeAscPolicyIdAsc();
}
