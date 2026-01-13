package ecommerce.controller;

import ecommerce.dtos.AddToCartRequest;
import ecommerce.dtos.CartResponseDTO;
import ecommerce.entity.Cart;
import ecommerce.entity.User;
import ecommerce.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<?> getCart(Authentication auth) {
        return ResponseEntity.ok(cartService.getUserCart(auth.getName()));
    }

    @PostMapping("/add")
    public ResponseEntity<CartResponseDTO> add(
            @RequestBody AddToCartRequest req,
            Authentication auth
    ) {
        return ResponseEntity.ok(
                cartService.addToCart(auth.getName(), req)
        );
    }


    @PutMapping("/increase/{productId}")
    public ResponseEntity<CartResponseDTO> increase(
            @PathVariable Long productId,
            Authentication auth
    ) {
        return ResponseEntity.ok(
                cartService.increaseQuantity(auth.getName(), productId)
        );
    }

    @PutMapping("/decrease/{productId}")
    public ResponseEntity<CartResponseDTO> decrease(
            @PathVariable Long productId,
            Authentication auth
    ) {
        return ResponseEntity.ok(
                cartService.decreaseQuantity(auth.getName(), productId)
        );
    }



}

