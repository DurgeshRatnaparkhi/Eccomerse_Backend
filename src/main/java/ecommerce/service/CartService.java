package ecommerce.service;

import ecommerce.dtos.AddToCartRequest;
import ecommerce.dtos.CartItemDTO;
import ecommerce.dtos.CartResponseDTO;
import ecommerce.entity.Cart;
import ecommerce.entity.CartItem;
import ecommerce.entity.Product;
import ecommerce.entity.User;
import ecommerce.repo.CartItemRepository;
import ecommerce.repo.CartRepository;
import ecommerce.repo.ProductRepository;
import ecommerce.repo.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CartService {

    private final CartRepository cartRepo;
    private final CartItemRepository cartItemRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;

    // ========================= ADD TO CART =========================
    public CartResponseDTO addToCart(String email, AddToCartRequest request) {

        log.info("Add to cart request for email={}, productId={}", email, request.getProductId());

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productRepo.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // ✅ Cart may or may not exist
        Cart cart = cartRepo.findByUser(user).orElse(null);

        if (cart == null) {
            log.info("Cart not found, creating new cart for userId={}", user.getId());
            cart = new Cart();
            cart.setUser(user);
            cart.setTotalAmount(BigDecimal.ZERO);
            cart = cartRepo.save(cart);
        }

        CartItem cartItem = cartItemRepo
                .findByCartAndProduct(cart, product)
                .orElse(null);

        if (cartItem == null) {
            cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setQuantity(request.getQuantity());
            cartItem.setPrice(product.getPrice());
        } else {
            cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
        }

        cartItem.setTotalPrice(
                cartItem.getPrice()
                        .multiply(BigDecimal.valueOf(cartItem.getQuantity()))
        );

        cartItemRepo.save(cartItem);

        updateCartTotal(cart);

        log.info("Product added to cart successfully for userId={}", user.getId());
        return mapToCartResponse(cart);
    }

    // ========================= GET USER CART =========================
    public CartResponseDTO getUserCart(String email) {

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Cart cart = cartRepo.findByUser(user).orElse(null);

        if (cart == null) {
            log.info("No cart found for userId={}", user.getId());
            CartResponseDTO empty = new CartResponseDTO();
            empty.setTotalAmount(BigDecimal.ZERO);
            empty.setItems(List.of());
            return empty;
        }

        return mapToCartResponse(cart);
    }

    // ========================= INCREASE QUANTITY =========================
    public CartResponseDTO increaseQuantity(String email, Long productId) {

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Cart cart = cartRepo.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        CartItem cartItem = cartItemRepo.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));

        cartItem.setQuantity(cartItem.getQuantity() + 1);
        cartItem.setTotalPrice(
                cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()))
        );

        cartItemRepo.save(cartItem);
        updateCartTotal(cart);

        log.info("Quantity increased for productId={} userId={}", productId, user.getId());
        return mapToCartResponse(cart);
    }

    // ========================= DECREASE QUANTITY =========================
    public CartResponseDTO decreaseQuantity(String email, Long productId) {

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Cart cart = cartRepo.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        CartItem cartItem = cartItemRepo.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));

        if (cartItem.getQuantity() == 1) {
            cartItemRepo.delete(cartItem);
            log.info("Item removed from cart productId={} userId={}", productId, user.getId());
        } else {
            cartItem.setQuantity(cartItem.getQuantity() - 1);
            cartItem.setTotalPrice(
                    cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()))
            );
            cartItemRepo.save(cartItem);
        }

        updateCartTotal(cart);
        return mapToCartResponse(cart);
    }

    // ========================= COMMON METHODS =========================
    private void updateCartTotal(Cart cart) {

        BigDecimal total = cartItemRepo.findByCart(cart).stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setTotalAmount(total);
        cartRepo.save(cart);
    }

    // ========================= ENTITY → DTO =========================
    private CartResponseDTO mapToCartResponse(Cart cart) {

        CartResponseDTO response = new CartResponseDTO();
        response.setCartId(cart.getId());
        response.setTotalAmount(cart.getTotalAmount());

        List<CartItemDTO> items = cartItemRepo.findByCart(cart).stream()
                .map(item -> {
                    CartItemDTO dto = new CartItemDTO();
                    dto.setProductId(item.getProduct().getId());
                    dto.setProductName(item.getProduct().getName());
                    dto.setPrice(item.getPrice());
                    dto.setQuantity(item.getQuantity());
                    dto.setTotalPrice(item.getTotalPrice());
                    return dto;
                }).toList();

        response.setItems(items);
        return response;
    }
}
