package ecommerce.controller;

import ecommerce.dtos.PaymentRequest;
import ecommerce.service.OrderService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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

        orderService.createOrderAfterPayment(request);

        Map<String,String> response = new HashMap<>();
        response.put("message","Payment verified and order placed");

        return ResponseEntity.ok(response);
    }

}