package ecommerce.controller;

import ecommerce.dtos.ResetPasswordRequest;
import ecommerce.service.ForgotPasswordService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("api/auth/forgot-password")
public class ForgotPasswordController {

    private final ForgotPasswordService forgotPasswordService;

    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, String>> sendOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        log.info("OTP request received for email: {}", email);

        String result = forgotPasswordService.sendOtpMail(email);

        log.info("OTP sent successfully to: {}", email);
        return ResponseEntity.ok(Map.of("message", result));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        log.info("OTP verification request for email: {}", email);

        String result = forgotPasswordService.verifyOtp(email, otp);

        log.info("OTP verification result for {}: {}", email, result);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        log.info("Password reset request for email: {}", request.getEmail());

        String response = forgotPasswordService.resetPassword(
                request.getEmail(),
                request.getOtp(),
                request.getNewPassword()
        );

        log.info("Password reset successful for {}", request.getEmail());
        return ResponseEntity.ok(response);
    }
}
