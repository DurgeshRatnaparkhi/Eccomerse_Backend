package ecommerce.dtos;

import lombok.Data;

@Data
public class PaymentRequest {

    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;

}