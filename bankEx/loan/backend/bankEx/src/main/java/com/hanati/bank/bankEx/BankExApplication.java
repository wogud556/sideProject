package com.hanati.bank.bankEx;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.hanati.bank.bankEx.loan")
public class BankExApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankExApplication.class, args);
	}

}
