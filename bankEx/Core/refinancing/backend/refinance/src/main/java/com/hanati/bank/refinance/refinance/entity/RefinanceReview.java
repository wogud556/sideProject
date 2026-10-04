package com.hanati.bank.refinance.refinance.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_REFINANCE_REVIEW")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefinanceReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    private Long applicationId;
    private String reviewerId;
    private LocalDateTime reviewedAt;

    @Lob
    private String eligibilitySnapshot;

    @Lob
    private String opinion;
}
