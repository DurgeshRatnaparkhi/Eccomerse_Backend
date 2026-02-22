package ecommerce.dtos;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponseDTO {
    private Long orderId;
    private LocalDateTime orderDate;
    private String status;
    private BigDecimal totalAmount;
    private String deliveryAddress;
    private List<OrderItemResponseDTO> items;
}

