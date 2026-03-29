package ecommerce.service;

import com.razorpay.RazorpayException;
import ecommerce.dtos.OrderResponseDTO;
import ecommerce.dtos.PaymentRequest;
import ecommerce.dtos.PlaceOrderRequestDTO;
import ecommerce.dtos.RazorpayOrderResponse;
import ecommerce.entity.User;

import java.util.List;

public interface OrderService {
    OrderResponseDTO placeOrder(User user, PlaceOrderRequestDTO request);

    RazorpayOrderResponse createRazorpayOrder(User user, Long addressId)
            throws RazorpayException;


    void createOrderAfterPayment(PaymentRequest request);
    List<OrderResponseDTO> getMyOrders(User user);


    OrderResponseDTO getOrderById(Long id, User user);

    void cancelOrder(Long id, User user);

    void updateOrderStatus(Long id, String status);

    List<OrderResponseDTO> getAllOrders();
}



