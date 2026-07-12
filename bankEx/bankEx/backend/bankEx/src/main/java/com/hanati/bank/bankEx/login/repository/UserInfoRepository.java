package com.hanati.bank.bankEx.login.repository;

import com.hanati.bank.bankEx.login.entity.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserInfoRepository extends JpaRepository<UserInfo, String> {
}
