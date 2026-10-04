package com.hanati.bank.refinance.customer.service;

import com.hanati.bank.refinance.audit.service.AuditLogService;
import com.hanati.bank.refinance.common.exception.BusinessException;
import com.hanati.bank.refinance.common.exception.ErrorCode;
import com.hanati.bank.refinance.customer.dto.CustomerResponse;
import com.hanati.bank.refinance.customer.entity.Customer;
import com.hanati.bank.refinance.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AuditLogService auditLogService;

    public List<CustomerResponse> search(String customerNo, String name, LocalDate birthDate, String phone, String operatorId) {
        List<CustomerResponse> results = customerRepository.search(customerNo, name, birthDate, phone).stream()
                .map(CustomerResponse::new)
                .toList();
        auditLogService.record(operatorId, "고객조회", null, null,
                "고객 검색 (결과 " + results.size() + "건)");
        return results;
    }

    public CustomerResponse get(Long customerId, String operatorId) {
        Customer customer = getCustomerOrThrow(customerId);
        auditLogService.record(operatorId, "고객조회", customerId, null, "고객 상세 조회");
        return new CustomerResponse(customer);
    }

    public Customer getCustomerOrThrow(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
    }
}
