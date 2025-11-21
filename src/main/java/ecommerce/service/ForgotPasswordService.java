package ecommerce.service;

import ecommerce.entity.PasswordResetToken;
import ecommerce.entity.User;
import ecommerce.repo.PasswordResetTokenRepository;
import ecommerce.repo.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForgotPasswordService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Transactional
    public String sendOtpMail(String email) {

        // Always convert email lowercase
        email = email.toLowerCase();

        userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not registered"));

        // Always 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));

        passwordResetTokenRepository.deleteByEmail(email);

        PasswordResetToken token = new PasswordResetToken(
                null,
                email,
                otp,
                LocalDateTime.now().plusMinutes(5),
                false
        );

        passwordResetTokenRepository.save(token);

        // Send mail
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Password Reset OTP");
        message.setText("Your OTP for password reset is: " + otp + "\nIt is valid for 5 minutes.");
        mailSender.send(message);

        log.info("OTP Sent to {} => {}", email, otp);

        return "OTP sent successfully";
    }

    @Transactional
    public String verifyOtp(String email, String otp) {

        email = email.toLowerCase();

        PasswordResetToken token = passwordResetTokenRepository.findByEmailAndOtp(email, otp)
                .orElseThrow(() -> new RuntimeException("Invalid OTP"));

        if (token.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        token.setVerified(true);   // mark OTP as verified
        passwordResetTokenRepository.save(token);

        return "OTP Verified";
    }


    @Transactional
    public String resetPassword(String email, String otp, String newPassword) {

        email = email.toLowerCase();

        PasswordResetToken token = passwordResetTokenRepository.findByEmailAndOtp(email, otp)
                .orElseThrow(() -> new RuntimeException("Invalid OTP"));

        // ❗ FIX — don't check expiry again
        if (!token.isVerified()) {
            throw new RuntimeException("OTP not verified");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not registered"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Clear token after successful reset
        passwordResetTokenRepository.deleteByEmail(email);
        SecurityContextHolder.clearContext();

        return "Password Reset Success";
    }

}
