package ecommerce.service;

import ecommerce.dtos.OrderResponseDTO;
import ecommerce.dtos.PlaceOrderRequestDTO;
import ecommerce.entity.User;

public interface OrderService {
    OrderResponseDTO placeOrder(User user, PlaceOrderRequestDTO request);
}

