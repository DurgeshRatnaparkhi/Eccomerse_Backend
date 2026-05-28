package ecommerce.controller;

import com.razorpay.Utils;
import ecommerce.entity.Order;
import ecommerce.enumm.OrderStatus;
import ecommerce.repo.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class RazorpayWebhookController {

    private final OrderRepository orderRepository;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {

        try {

            // ✅ STEP 1: Verify Signature
            boolean isValid = Utils.verifyWebhookSignature(
                    payload,
                    signature,
                    webhookSecret
            );

            if (!isValid) {
                log.error("Invalid webhook signature");
                return ResponseEntity.badRequest().body("Invalid signature");
            }

            // ✅ STEP 2: Convert payload to JSON
            JSONObject json = new JSONObject(payload);

            String event = json.getString("event");
            log.info("Webhook event: {}", event);

            JSONObject payloadObj = json.getJSONObject("payload");

            // =========================================
            // HANDLE PAYMENT SUCCESS
            // =========================================
            if ("payment.captured".equals(event)) {

                JSONObject paymentEntity = payloadObj
                        .getJSONObject("payment")
                        .getJSONObject("entity");

                String razorpayOrderId = paymentEntity.getString("order_id");
                String paymentId = paymentEntity.getString("id");

                Order order = orderRepository
                        .findByRazorpayOrderId(razorpayOrderId)
                        .orElseThrow(() ->
                                new RuntimeException("Order not found"));

                order.setPaymentId(paymentId);
                order.setOrderStatus(OrderStatus.PAID);

                orderRepository.save(order);

                log.info("Order marked as PAID: {}", razorpayOrderId);
            }

            // =========================================
            // HANDLE PAYMENT FAILURE
            // =========================================
            if ("payment.failed".equals(event)) {

                JSONObject paymentEntity = payloadObj
                        .getJSONObject("payment")
                        .getJSONObject("entity");

                String razorpayOrderId = paymentEntity.getString("order_id");

                Order order = orderRepository
                        .findByRazorpayOrderId(razorpayOrderId)
                        .orElseThrow(() ->
                                new RuntimeException("Order not found"));

                order.setOrderStatus(OrderStatus.FAILED);

                orderRepository.save(order);

                log.info("Order marked as FAILED: {}", razorpayOrderId);
            }

            return ResponseEntity.ok("Webhook processed successfully");

        } catch (Exception e) {
            log.error("Webhook error", e);
            return ResponseEntity.internalServerError().body("Webhook error");
        }
    }
}