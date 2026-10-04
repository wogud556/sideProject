package com.hanati.bank.screening.repository;

import com.hanati.bank.screening.entity.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserInfoRepository extends JpaRepository<UserInfo, String> {
}
