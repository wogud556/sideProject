package com.hanati.bank.refinance.operator.repository;

import com.hanati.bank.refinance.operator.entity.Operator;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperatorRepository extends JpaRepository<Operator, String> {
}
