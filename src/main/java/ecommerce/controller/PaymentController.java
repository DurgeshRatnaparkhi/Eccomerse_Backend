package ecommerce.controller;

import ecommerce.dtos.PaymentRequest;
import ecommerce.service.OrderService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final OrderService orderService;

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody PaymentRequest request) {

        boolean isValid = orderService.verifySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        if(isValid){
            orderService.createOrderAfterPayment(request);
        } else {
            orderService.handlePaymentFailure(request.getRazorpayOrderId());
            throw new RuntimeException("Invalid payment signature");
        }

        Map<String,String> response = new HashMap<>();
        response.put("message","Payment verified successfully");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/failure")
    public ResponseEntity<?> paymentFailed(@RequestBody Map<String, String> data) {

        orderService.handlePaymentFailure(data.get("razorpayOrderId"));

        return ResponseEntity.ok("Payment failed updated");
    }


}