package ecommerce.service.impl;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import ecommerce.dtos.OrderItemResponseDTO;
import ecommerce.dtos.OrderResponseDTO;
import ecommerce.dtos.PlaceOrderRequestDTO;
import ecommerce.dtos.RazorpayOrderResponse;
import ecommerce.entity.*;
import ecommerce.enumm.OrderStatus;
import ecommerce.enumm.PaymentStatus;
import ecommerce.exception.AddressNotFoundException;
import ecommerce.exception.CartEmptyException;
import ecommerce.repo.AddressRepository;
import ecommerce.repo.CartRepository;
import ecommerce.repo.OrderRepository;
import ecommerce.service.OrderService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
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
    private final RazorpayClient razorpayClient;

    // ======================================================
    // NORMAL ORDER (WITHOUT PAYMENT)
    // ======================================================
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
        order.setOrderStatus(OrderStatus.CREATED);
        order.setPaymentStatus(PaymentStatus.PENDING);

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {

            Product product = cartItem.getProduct();
            Integer stock = product.getStock();

            // ✅ NULL SAFE STOCK CHECK
            if (stock == null || stock < cartItem.getQuantity()) {
                throw new RuntimeException("Product " + product.getName() + " is out of stock");
            }

            // ✅ REDUCE STOCK
            product.setStock(stock - cartItem.getQuantity());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(product.getPrice());

            BigDecimal itemTotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            orderItem.setTotalPrice(itemTotal);

            totalAmount = totalAmount.add(itemTotal);
            orderItems.add(orderItem);
        }

        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        cart.getItems().clear();

        log.info("Order placed successfully orderId={}", savedOrder.getId());

        return mapToOrderResponseDTO(savedOrder);
    }

    // ======================================================
    // RAZORPAY PAYMENT FLOW
    // ======================================================
    public RazorpayOrderResponse createRazorpayOrder(User user, Long addressId)
            throws RazorpayException {

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new CartEmptyException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new CartEmptyException("Cart is empty");
        }

        Address address = addressRepository.findByIdAndUser(addressId, user)
                .orElseThrow(() -> new AddressNotFoundException("Invalid address"));

        BigDecimal totalAmount = cart.getItems().stream()
                .map(item -> item.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = new Order();
        order.setUser(user);
        order.setAddress(address);
        order.setOrderStatus(OrderStatus.CREATED);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setTotalAmount(totalAmount);
        order.setOrderDate(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        JSONObject options = new JSONObject();
        options.put("amount", totalAmount.multiply(BigDecimal.valueOf(100)).intValue()); // paise
        options.put("currency", "INR");
        options.put("receipt", "order_" + savedOrder.getId());

        com.razorpay.Order razorpayOrder = razorpayClient.orders.create(options);

        savedOrder.setRazorpayOrderId(razorpayOrder.get("id").toString());
        orderRepository.save(savedOrder);

        return new RazorpayOrderResponse(
                savedOrder.getId(),
                razorpayOrder.get("id").toString(),
                totalAmount
        );
    }

    // ======================================================
    // MAPPER
    // ======================================================
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
                .status(order.getOrderStatus())
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