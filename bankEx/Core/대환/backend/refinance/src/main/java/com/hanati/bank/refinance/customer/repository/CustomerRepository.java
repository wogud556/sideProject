package com.hanati.bank.refinance.customer.repository;

import com.hanati.bank.refinance.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query("SELECT c FROM Customer c WHERE " +
            "(:customerNo IS NULL OR c.customerNo = :customerNo) AND " +
            "(:name IS NULL OR c.name = :name) AND " +
            "(:birthDate IS NULL OR c.birthDate = :birthDate) AND " +
            "(:phone IS NULL OR c.phone = :phone)")
    List<Customer> search(@Param("customerNo") String customerNo,
                           @Param("name") String name,
                           @Param("birthDate") LocalDate birthDate,
                           @Param("phone") String phone);
}
