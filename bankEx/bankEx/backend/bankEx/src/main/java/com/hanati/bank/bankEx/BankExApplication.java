package com.hanati.bank.bankEx;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan({"com.hanati.bank.bankEx.loan", "com.hanati.bank.bankEx.deposit.savings"})
public class BankExApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankExApplication.class, args);
	}

}
