package com.hanati.bank.mortgage.operator;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "MORTGAGE_OPERATOR")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Operator {

    @Id
    @Column(name = "OPERATOR_ID", length = 20)
    private String operatorId;

    @Column(name = "OPERATOR_NAME", nullable = false, length = 50)
    private String operatorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", nullable = false, length = 20)
    private OperatorRole role;
}
