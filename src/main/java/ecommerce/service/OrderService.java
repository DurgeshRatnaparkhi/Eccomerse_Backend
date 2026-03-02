package ecommerce.service;

import com.razorpay.RazorpayException;
import ecommerce.dtos.OrderResponseDTO;
import ecommerce.dtos.PlaceOrderRequestDTO;
import ecommerce.dtos.RazorpayOrderResponse;
import ecommerce.entity.User;

public interface OrderService {
    OrderResponseDTO placeOrder(User user, PlaceOrderRequestDTO request);

    RazorpayOrderResponse createRazorpayOrder(User user, Long addressId)
            throws RazorpayException;

}

