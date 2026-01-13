package ecommerce.dtos;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CartResponseDTO {
    private Long cartId;
    private BigDecimal totalAmount;
    private List<CartItemDTO> items;
}
