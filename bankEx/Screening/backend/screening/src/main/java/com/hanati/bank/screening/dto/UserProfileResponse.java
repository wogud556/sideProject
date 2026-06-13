package com.hanati.bank.screening.dto;

import com.hanati.bank.screening.entity.CustomerCreditInfo;
import com.hanati.bank.screening.entity.UserInfo;
import lombok.Getter;

@Getter
public class UserProfileResponse {
    private String userId;
    private String userName;
    private String phoneNumber;
    private Integer creditScore;
    private Long annualIncome;
    private String employmentType;
    private String companyName;
    private Integer employmentMonths;
    private Long existingDebt;
    private Long annualRepayment;

    public UserProfileResponse(UserInfo u, CustomerCreditInfo c) {
        this.userId = u.getUserId();
        this.userName = u.getUserName();
        this.phoneNumber = u.getPhoneNumber();
        if (c != null) {
            this.creditScore = c.getCreditScore();
            this.annualIncome = c.getAnnualIncome();
            this.employmentType = c.getEmploymentType();
            this.companyName = c.getCompanyName();
            this.employmentMonths = c.getEmploymentMonths();
            this.existingDebt = c.getExistingDebt();
            this.annualRepayment = c.getAnnualRepayment();
        }
    }
}
