package com.hanati.bank.bankEx.deposit.savings.domain;

import com.hanati.bank.bankEx.deposit.savings.enums.AutoTransferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoTransfer {
    private Long autoId;
    private String accountNo;
    private String withdrawAccount;
    private Integer transferDay;

    @Setter
    private AutoTransferStatus status;

    private LocalDateTime createdAt;
}
