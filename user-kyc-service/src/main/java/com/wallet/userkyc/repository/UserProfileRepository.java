package com.wallet.userkyc.repository;

import com.wallet.userkyc.entity.UserProfile;



//Ye ek interface hai jo already ready-made DB operations deta hai


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
/*

UserProfile
 Kaunsi table pe kaam karna hai

 */


/*
Long= Primary key ka type
 */

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
	Optional<UserProfile> findByAuthUserId(Long authUserId);
	Optional<UserProfile> findByPhone(String phone);
	Optional<UserProfile> findByUpiId(String upiId);
}

