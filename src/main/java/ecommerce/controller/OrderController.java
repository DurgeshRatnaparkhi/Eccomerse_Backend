package ecommerce.controller;


import com.razorpay.RazorpayException;
import ecommerce.dtos.DashboardDTO;
import ecommerce.dtos.OrderResponseDTO;
import ecommerce.dtos.PlaceOrderRequestDTO;
import ecommerce.entity.Order;
import ecommerce.entity.User;
import ecommerce.enumm.OrderStatus;
import ecommerce.repo.OrderRepository;
import ecommerce.repo.UserRepository;
import ecommerce.service.DashboardService;
import ecommerce.service.InvoiceService;
import ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;
    private final DashboardService dashboardService;
    private final OrderRepository orderRepository;
    private final InvoiceService invoiceService;

    @PostMapping("/place")
    public ResponseEntity<OrderResponseDTO> placeOrder(
            @RequestBody PlaceOrderRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("Place order API called for userId={}", user.getId());

        OrderResponseDTO response = orderService.placeOrder(user, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody PlaceOrderRequestDTO request
    ) throws RazorpayException {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(
                orderService.createRazorpayOrder(user, request.getAddressId())
        );
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(orderService.getMyOrders(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(orderService.getOrderById(id, user));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        orderService.cancelOrder(id, user);

        return ResponseEntity.ok("Order cancelled successfully");
    }


    // ================= ADMIN APIs =================

    // ✅ GET ALL ORDERS (ADMIN)
    @GetMapping("/admin/orders")
    public ResponseEntity<?> getAllOrders() {

        return ResponseEntity.ok(orderService.getAllOrders());
    }


    // ✅ UPDATE ORDER STATUS (ADMIN)
    @PutMapping("/admin/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String status = body.get("status");

        orderService.updateOrderStatus(id, status);

        return ResponseEntity.ok(Map.of(
                "message", "Order status updated successfully"
        ));
    }

    @GetMapping("/admin/dashboard")
    public ResponseEntity<DashboardDTO> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboardStatus());
    }

    @GetMapping("/admin/orders-chart")
    public ResponseEntity<?> getOrderChart(){

        return ResponseEntity.ok(dashboardService.getOrdersPerDay());
    }


    @GetMapping("/invoice/{orderId}")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if(order.getOrderStatus() != OrderStatus.PLACED){
            throw new RuntimeException("Invoice available only for placed orders");
        }

        byte[] pdf = invoiceService.generateInvoice(order);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=invoice.pdf")
                .body(pdf);
    }

}