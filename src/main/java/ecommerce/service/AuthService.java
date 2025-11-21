package ecommerce.service;

import ecommerce.dtos.AuthRequest;
import ecommerce.dtos.AuthResponse;
import ecommerce.dtos.RegisterRequest;
import ecommerce.entity.Role;
import ecommerce.entity.User;
import ecommerce.repo.UserRepository;
import ecommerce.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // ✅ Register user (no token generation here)
    public String register(RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return "Email already exists!";
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))  // encoded password
                .role(Role.USER) // default role
                .build();

        userRepository.save(user);

        return "Register successful!";
    }

    // ✅ Login user and generate JWT token
    @Transactional
    public AuthResponse login(AuthRequest request) {

        log.info("Authenticating user: {}", request.getEmail());

        // Authentication check
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Always fetch the latest user record
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("User authenticated successfully: {}", request.getEmail());

        // Generate JWT
        String token = jwtService.generateToken(new CustomUserDetails(user), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .name(user.getName())
                .role(user.getRole().name())
                .build();
    }
}
