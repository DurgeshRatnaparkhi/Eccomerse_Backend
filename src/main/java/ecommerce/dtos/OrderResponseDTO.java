package ecommerce.dtos;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponseDTO {
    private Long orderId;
    private LocalDateTime orderDate;
    private String status;
    private double totalAmount;
    private String deliveryAddress;
    private List<OrderItemResponseDTO> items;
}

