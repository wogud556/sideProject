package com.hanati.bank.bankEx.repository;

import com.hanati.bank.bankEx.entity.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserInfoRepository extends JpaRepository<UserInfo, String> {
}
