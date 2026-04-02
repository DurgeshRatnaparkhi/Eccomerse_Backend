package ecommerce.service.impl;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import ecommerce.dtos.*;
import ecommerce.entity.*;
import ecommerce.enumm.OrderStatus;
import ecommerce.enumm.PaymentStatus;
import ecommerce.exception.AddressNotFoundException;
import ecommerce.exception.CartEmptyException;
import ecommerce.exception.OrderNotFoundException;
import ecommerce.repo.AddressRepository;
import ecommerce.repo.CartRepository;
import ecommerce.repo.OrderRepository;
import ecommerce.service.EmailService;
import ecommerce.service.OrderService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Transactional
@Service
public class OrderServiceImpl implements OrderService {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final RazorpayClient razorpayClient;
    private final EmailService emailService;


    @Value("${razorpay.key.secret}")
    private String secret;
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

        emailService.sendEmail(
                user.getEmail(),
                "Order Placed Successfully",
                "Your order #" + order.getId() + " has been placed successfully."
        );

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
                .userName(order.getUser().getName())
                .deliveryAddress(
                        addr.getStreet() + ", " +
                                addr.getCity() + ", " +
                                addr.getState() + " - " +
                                addr.getPincode())
                .items(itemDTOs)
                .build();
    }

    public void createOrderAfterPayment(PaymentRequest request){

        log.info(" Creating order after successful payment for razorpayOrderId={}", request.getRazorpayOrderId());


        Order order = orderRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new RuntimeException(" Order not found"));

        if(order.getPaymentStatus() == PaymentStatus.SUCCESS){
            log.warn("Order already processed, skipping duplicate payment");
            return;
        }

        User user = order.getUser();

        Cart cart = cartRepository.findByUser(user).
                orElseThrow(() -> new RuntimeException("Cart not found"));


        List<OrderItem> orderItems = order.getItems();

        if(orderItems == null){
            orderItems = new ArrayList<>();
            order.setItems(orderItems);
        }

        for(CartItem cartItem : cart.getItems()){

            Product product = cartItem.getProduct();

            // check stock
            if(product.getStock() < cartItem.getQuantity()){
                throw new RuntimeException(product.getName()+" out of stock");
            }

            // decrease stock
            product.setStock(product.getStock() - cartItem.getQuantity());

            OrderItem orderItem = new OrderItem();

            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(product.getPrice());

            BigDecimal total =
                    product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            orderItem.setTotalPrice(total);

            orderItems.add(orderItem);
        }

        order.setItems(orderItems);

        order.setPaymentStatus(PaymentStatus.SUCCESS);
        order.setOrderStatus(OrderStatus.PLACED);
        order.setPaymentId(request.getRazorpayPaymentId());

        orderRepository.save(order);

        // clear cart
        cart.getItems().clear();
        cart.setTotalAmount(BigDecimal.ZERO);

        log.info("Payment verified and order placed successfully orderId={}", order.getId());
    }

    //failed payment handler

    public void handlePaymentFailure(String razorpayOrderId){

        log.info("Payment failed for razorpayOrderId={}", razorpayOrderId);

        Order order = orderRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // 🔥 IMPORTANT FIX (DO NOT OVERRIDE SUCCESS)
        if(order.getPaymentStatus() == PaymentStatus.SUCCESS){
            return; // ✅ IGNORE FAILURE (already paid)
        }

        order.setPaymentStatus(PaymentStatus.FAILED);
        order.setOrderStatus(OrderStatus.FAILED);

        orderRepository.save(order);
    }



    @Override
    public boolean verifySignature(String orderId, String paymentId, String signature) {

        try {
            String data = orderId + "|" + paymentId;

            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey =
                    new javax.crypto.spec.SecretKeySpec(secret.getBytes(), "HmacSHA256");

            mac.init(secretKey);

            byte[] rawHmac = mac.doFinal(data.getBytes());

            String generatedSignature = bytesToHex(rawHmac);

            return generatedSignature.equals(signature);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hex = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String s = Integer.toHexString(0xff & b);
            if (s.length() == 1) hex.append('0');
            hex.append(s);
        }
        return hex.toString();
    }

    @Override
    public List<OrderResponseDTO> getMyOrders(User user) {

        List<Order> orders = orderRepository.findByUser(user);

        return orders.stream()
                .map(this::mapToOrderResponseDTO)
                .toList();
    }

    public OrderResponseDTO getOrderById(Long id, User user) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // 🔐 SECURITY CHECK
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access");
        }

        return mapToOrderResponseDTO(order); // ✅ SINGLE OBJECT
    }


    public void cancelOrder(Long id, User user) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        // 🔐 Check ownership
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        // ❌ Cannot cancel after shipping
        if (order.getOrderStatus() == OrderStatus.SHIPPED ||
                order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new RuntimeException("Order cannot be cancelled now");
        }

        // 🔄 Restore stock
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
        }

        // 🔴 Update status
        order.setOrderStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }


    //get all orders for admin
    public List<OrderResponseDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToOrderResponseDTO)
                .toList();

    }


    // ✅ UPDATE STATUS
    public void updateOrderStatus(Long id, String status) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        OrderStatus newStatus = OrderStatus.valueOf(status);

// ❌ Block invalid transitions
        if(order.getOrderStatus() == OrderStatus.FAILED){
            throw new RuntimeException("Cannot update FAILED order");
        }

        if(order.getOrderStatus() == OrderStatus.CANCELLED){
            throw new RuntimeException("Cannot update CANCELLED order");
        }

// ❌ Prevent jumping directly to SHIPPED
        if(newStatus == OrderStatus.SHIPPED && order.getOrderStatus() != OrderStatus.PLACED){
            throw new RuntimeException("Order must be PLACED before SHIPPING");
        }

        order.setOrderStatus(newStatus);
        orderRepository.save(order);

        // ✅ SEND EMAIL BASED ON STATUS
        if (newStatus == OrderStatus.SHIPPED) {

            emailService.sendEmail(
                    order.getUser().getEmail(),
                    "Order Shipped",
                    "Your order #" + order.getId() + " has been shipped."
            );
        }

        if (newStatus == OrderStatus.DELIVERED) {

            emailService.sendEmail(
                    order.getUser().getEmail(),
                    "Order Delivered",
                    "Your order #" + order.getId() + " has been delivered."
            );
        }


    }



}
