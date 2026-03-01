package ecommerce.controller;
import ecommerce.dtos.AddressDto;
import ecommerce.entity.User;
import ecommerce.repo.UserRepository;
import ecommerce.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/addresses")
@RequiredArgsConstructor
@CrossOrigin
public class AddressController {

    private final AddressService addressService;

    private final UserRepository userRepository;

    @PostMapping
    public AddressDto addAddress(@RequestBody AddressDto dto) {
        User user = getLoggedInUser();
        return addressService.addAddress(dto, user);
    }

    @GetMapping
    public List<AddressDto> getMyAddresses() {
        User user = getLoggedInUser();
        return addressService.getUserAddresses(user);
    }

    @DeleteMapping("/{id}")
    public void deleteAddress(@PathVariable Long id) {
        User user = getLoggedInUser();
        addressService.deleteAddress(id, user);
    }

    private User getLoggedInUser() {
        Authentication authentication = SecurityContextHolder
                .getContext().
                getAuthentication();
        String email = authentication.getName();//email stored in token

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }
}