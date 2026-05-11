package com.gemstore.backend.services.auth;

import com.gemstore.backend.entities.user.EmailVerificationOtp;
import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.repositories.user.EmailVerificationOtpRepository;
import com.gemstore.backend.repositories.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class EmailVerificationOtpService {

    private final EmailVerificationOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional
    public void generateAndSendOtp(User user) {

        otpRepository.deleteByUserId(user.getId());

        String otp = String.format("%06d",
                new Random().nextInt(999999));

        EmailVerificationOtp entity =
                EmailVerificationOtp.builder()
                        .user(user)
                        .otpCode(otp)
                        .expiresAt(Instant.now().plusSeconds(300))
                        .build();

        otpRepository.save(entity);

        emailService.sendOtpEmail(user.getEmail(), otp);
    }

    @Transactional
    public void verifyOtp(Long userId, String otp) {

        EmailVerificationOtp entity =
                otpRepository.findByUserId(userId)
                        .orElseThrow(() ->
                                new IllegalArgumentException("OTP not found"));

        if (entity.isExpired()) {
            throw new IllegalArgumentException("OTP expired");
        }

        if (!entity.getOtpCode().equals(otp)) {

            entity.setAttempts(entity.getAttempts() + 1);

            otpRepository.save(entity);

            throw new IllegalArgumentException("Invalid OTP");
        }

        User user = entity.getUser();

        user.setEmailVerified(true);

        userRepository.save(user);

        otpRepository.delete(entity);
    }
}