package com.hanati.bank.refinance.refinance.service;

import com.hanati.bank.refinance.customer.entity.Customer;
import com.hanati.bank.refinance.customer.repository.CustomerRepository;
import com.hanati.bank.refinance.loan.entity.ExistingLoan;
import com.hanati.bank.refinance.loan.repository.ExistingLoanRepository;
import com.hanati.bank.refinance.refinance.domain.RefinanceStatus;
import com.hanati.bank.refinance.refinance.dto.EligibilityResponse;
import com.hanati.bank.refinance.refinance.entity.RefinanceApplication;
import com.hanati.bank.refinance.refinance.repository.RefinanceApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefinanceEligibilityServiceTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ExistingLoanRepository existingLoanRepository;
    @Mock
    private RefinanceApplicationRepository refinanceApplicationRepository;

    @InjectMocks
    private RefinanceEligibilityService eligibilityService;

    private Customer activeCustomer() {
        return Customer.builder().customerId(1L).status("ACTIVE").build();
    }

    private ExistingLoan normalLoan() {
        return ExistingLoan.builder()
                .loanId(10L).customerId(1L).status("ACTIVE")
                .currentBalance(new BigDecimal("1000000"))
                .overdueYn("N")
                .maturityDate(LocalDate.now().plusYears(1))
                .build();
    }

    @Test
    void eligible_when_customer_and_loan_are_all_normal() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(activeCustomer()));
        when(refinanceApplicationRepository.findByCustomerIdAndStatusIn(anyLong(), any())).thenReturn(List.of());
        when(existingLoanRepository.findById(10L)).thenReturn(Optional.of(normalLoan()));

        EligibilityResponse response = eligibilityService.check(1L, List.of(10L));

        assertTrue(response.eligible());
    }

    @Test
    void not_eligible_when_loan_is_overdue() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(activeCustomer()));
        when(refinanceApplicationRepository.findByCustomerIdAndStatusIn(anyLong(), any())).thenReturn(List.of());
        ExistingLoan overdueLoan = ExistingLoan.builder()
                .loanId(10L).customerId(1L).status("ACTIVE")
                .currentBalance(new BigDecimal("1000000"))
                .overdueYn("Y")
                .maturityDate(LocalDate.now().plusYears(1))
                .build();
        when(existingLoanRepository.findById(10L)).thenReturn(Optional.of(overdueLoan));

        EligibilityResponse response = eligibilityService.check(1L, List.of(10L));

        assertFalse(response.eligible());
        assertTrue(response.results().stream().anyMatch(r -> "OVERDUE_LOAN".equals(r.code()) && !r.passed()));
    }

    @Test
    void not_eligible_when_loan_balance_is_zero() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(activeCustomer()));
        when(refinanceApplicationRepository.findByCustomerIdAndStatusIn(anyLong(), any())).thenReturn(List.of());
        ExistingLoan zeroBalanceLoan = ExistingLoan.builder()
                .loanId(10L).customerId(1L).status("ACTIVE")
                .currentBalance(BigDecimal.ZERO)
                .overdueYn("N")
                .maturityDate(LocalDate.now().plusYears(1))
                .build();
        when(existingLoanRepository.findById(10L)).thenReturn(Optional.of(zeroBalanceLoan));

        EligibilityResponse response = eligibilityService.check(1L, List.of(10L));

        assertFalse(response.eligible());
        assertTrue(response.results().stream().anyMatch(r -> "LOAN_BALANCE".equals(r.code()) && !r.passed()));
    }

    @Test
    void not_eligible_when_another_application_is_already_in_progress() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(activeCustomer()));
        RefinanceApplication inProgress = RefinanceApplication.builder()
                .applicationId(99L).status(RefinanceStatus.REVIEWING).build();
        when(refinanceApplicationRepository.findByCustomerIdAndStatusIn(anyLong(), any())).thenReturn(List.of(inProgress));
        when(existingLoanRepository.findById(10L)).thenReturn(Optional.of(normalLoan()));

        EligibilityResponse response = eligibilityService.check(1L, List.of(10L));

        assertFalse(response.eligible());
    }

    @Test
    void excludes_the_current_application_itself_from_the_in_progress_check() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(activeCustomer()));
        RefinanceApplication self = RefinanceApplication.builder()
                .applicationId(99L).status(RefinanceStatus.REVIEWING).build();
        when(refinanceApplicationRepository.findByCustomerIdAndStatusIn(anyLong(), any())).thenReturn(List.of(self));
        when(existingLoanRepository.findById(10L)).thenReturn(Optional.of(normalLoan()));

        EligibilityResponse response = eligibilityService.check(1L, List.of(10L), 99L);

        assertTrue(response.eligible());
    }
}
