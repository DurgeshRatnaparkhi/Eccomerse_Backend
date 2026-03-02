package ecommerce.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class RazorpayOrderResponse {

    private Long orderId;              // Your DB order ID
    private String razorpayOrderId;    // Razorpay generated order ID
    private BigDecimal totalAmount;    // Total amount
}