package ecommerce.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;
@Data
public class ProductResponseDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer quantityAvailable;
    private String brand;
    private String category;
    private String sku;
    private String imageUrl;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
