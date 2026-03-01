package ecommerce.service.impl;

import ecommerce.dtos.OrderItemResponseDTO;
import ecommerce.dtos.OrderResponseDTO;
import ecommerce.dtos.PlaceOrderRequestDTO;
import ecommerce.entity.*;
import ecommerce.exception.AddressNotFoundException;
import ecommerce.exception.CartEmptyException;
import ecommerce.repo.AddressRepository;
import ecommerce.repo.CartRepository;
import ecommerce.repo.OrderRepository;
import ecommerce.service.OrderService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
@AllArgsConstructor
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;



    @Override
    public OrderResponseDTO placeOrder(User user, PlaceOrderRequestDTO request) {

        log.info("Placing order for userId={}", user.getId());

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new CartEmptyException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new CartEmptyException("Cart is empty");
        }

        Address address = addressRepository.findByIdAndUser(request.getAddressId(), user)
                .orElseThrow(() -> new AddressNotFoundException("Address Invalid"));

        Order order = new Order();
        order.setUser(user);
        order.setAddress(address);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("PLACED");
        order.setPaymentStatus("PENDING");

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {

            OrderItem orderItem = new OrderItem();

            if (cartItem.getProduct().getStock() < cartItem.getQuantity()) {
                throw new RuntimeException("Product " + cartItem.getProduct().getName() + " is out of stock");
            }
            orderItem.setOrder(order);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getProduct().getPrice());

            BigDecimal itemTotal =
                    cartItem.getProduct().getPrice()
                            .multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            orderItem.setTotalPrice(itemTotal);

            totalAmount = totalAmount.add(itemTotal);
            orderItems.add(orderItem);
        }

        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        cart.getItems().clear(); // clear cart after order

        log.info("Order placed successfully orderId={}", savedOrder.getId());

        return mapToOrderResponseDTO(savedOrder);
    }

    private OrderResponseDTO mapToOrderResponseDTO(Order order) {

        List<OrderItemResponseDTO> itemDTOs = order.getItems().stream()
                .map(item -> OrderItemResponseDTO.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .toList();

        Address addr = order.getAddress();

        return OrderResponseDTO.builder()
                .orderId(order.getId())
                .orderDate(order.getOrderDate())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .deliveryAddress(
                        addr.getStreet() + ", " +
                                addr.getCity() + ", " +
                                addr.getState() + " - " +
                                addr.getPincode())
                .items(itemDTOs)
                .build();

    }
}
