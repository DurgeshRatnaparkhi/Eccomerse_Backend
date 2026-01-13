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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepo;
    private final CartItemRepository cartItemRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;

    public CartResponseDTO addToCart(String email, AddToCartRequest request) {

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productRepo.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Cart cart = cartRepo.findByUser(user);

        // ✅ Create cart if not exists
        if (cart == null) {
            cart = new Cart();
            cart.setUser(user);
            cart.setTotalAmount(BigDecimal.ZERO);
            cart = cartRepo.save(cart);
        }

        // ✅ Find existing cart item
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

        // ✅ Recalculate cart total
        BigDecimal total = cartItemRepo.findByCart(cart).stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add );

        cart.setTotalAmount(total);
        cartRepo.save(cart);

        // ✅ Convert ENTITY → DTO (MOST IMPORTANT PART)
        return mapToCartResponse(cart);
    }

    public CartResponseDTO getUserCart(String email) {

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Cart cart = cartRepo.findByUser(user);

        if (cart == null) {
            CartResponseDTO empty = new CartResponseDTO();
            empty.setTotalAmount(BigDecimal.ZERO);
            empty.setItems(List.of());
            return empty;
        }

        return mapToCartResponse(cart);
    }

    // 🔥 Mapper method (ENTITY → DTO)
    private CartResponseDTO mapToCartResponse(Cart cart) {

        CartResponseDTO response = new CartResponseDTO();
        response.setCartId(cart.getId());
        response.setTotalAmount(cart.getTotalAmount());

        List<CartItemDTO> items = cartItemRepo.findByCart(cart).stream().map(item -> {
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


    public CartResponseDTO increaseQuantity(String email, Long productId) {

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Cart cart = cartRepo.findByUser(user);
        if (cart == null) {
            throw new RuntimeException("Cart not found");
        }

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

        return mapToCartResponse(cart);
    }

    public CartResponseDTO decreaseQuantity(String email, Long productId) {

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Cart cart = cartRepo.findByUser(user);
        if (cart == null) {
            throw new RuntimeException("Cart not found");
        }

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        CartItem cartItem = cartItemRepo.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));

        if (cartItem.getQuantity() == 1) {
            cartItemRepo.delete(cartItem);
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

    // ♻ common method
    private void updateCartTotal(Cart cart) {
        BigDecimal total = cartItemRepo.findByCart(cart).stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setTotalAmount(total);
        cartRepo.save(cart);
    }

}
