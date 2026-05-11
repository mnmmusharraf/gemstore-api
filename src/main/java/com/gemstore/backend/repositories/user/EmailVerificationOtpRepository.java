package com.gemstore.backend.repositories.user;

import com.gemstore.backend.entities.user.EmailVerificationOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationOtpRepository
        extends JpaRepository<EmailVerificationOtp, Long> {

    Optional<EmailVerificationOtp> findByUserId(Long userId);

    Optional<EmailVerificationOtp> findByUserEmailIgnoreCase(String email);

    void deleteByUserId(Long userId);
}