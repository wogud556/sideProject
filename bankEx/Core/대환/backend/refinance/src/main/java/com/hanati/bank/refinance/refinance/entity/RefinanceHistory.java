package com.hanati.bank.refinance.refinance.entity;

import com.hanati.bank.refinance.refinance.domain.RefinanceStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_REFINANCE_HISTORY")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefinanceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyId;

    private Long applicationId;
    private String actionType;

    @Enumerated(EnumType.STRING)
    private RefinanceStatus fromStatus;

    @Enumerated(EnumType.STRING)
    private RefinanceStatus toStatus;

    private String description;
    private String processedBy;
    private LocalDateTime processedAt;
}
