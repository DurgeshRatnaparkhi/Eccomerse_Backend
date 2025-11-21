package ecommerce.security;

import ecommerce.entity.User;
import ecommerce.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        log.info("Loading user by email : {}",email);
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("user not found: " + email));
        log.info("user found with role: {}",user.getRole());
        return new CustomUserDetails(user);
    }
}
