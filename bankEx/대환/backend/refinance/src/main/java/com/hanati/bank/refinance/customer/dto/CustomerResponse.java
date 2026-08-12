package com.hanati.bank.refinance.customer.dto;

import com.hanati.bank.refinance.common.util.MaskUtil;
import com.hanati.bank.refinance.customer.entity.Customer;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
public class CustomerResponse {

    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private final Long customerId;
    private final String customerNo;
    private final String maskedName;
    private final String maskedPhone;
    private final String maskedBirthDate;
    private final String status;

    public CustomerResponse(Customer customer) {
        this.customerId = customer.getCustomerId();
        this.customerNo = customer.getCustomerNo();
        this.maskedName = MaskUtil.maskName(customer.getName());
        this.maskedPhone = MaskUtil.maskPhone(customer.getPhone());
        this.maskedBirthDate = customer.getBirthDate() == null
                ? null
                : customer.getBirthDate().format(YEAR_MONTH) + "-**";
        this.status = customer.getStatus();
    }
}
