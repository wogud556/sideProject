package com.hanati.bank.refinance.operator.entity;

import com.hanati.bank.refinance.operator.enums.OperatorRole;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "TB_REFINANCE_OPERATOR")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Operator {

    @Id
    private String operatorId;

    private String name;

    @Enumerated(EnumType.STRING)
    private OperatorRole role;
}
