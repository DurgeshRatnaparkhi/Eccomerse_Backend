package ecommerce.service.Impl;

import ecommerce.dtos.ProductRequestDTO;
import ecommerce.dtos.ProductResponseDTO;
import ecommerce.entity.Product;
import ecommerce.repo.ProductRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class ProductServiceImpl implements ProductService {


    private final ProductRepository productRepository;


    @Override
    public ProductResponseDTO addProduct(ProductRequestDTO dto) {
        log.info("product adding new : {}", dto.getName());
        Product product = Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .quantityAvailable(dto.getQuantityAvailable())
                .brand(dto.getBrand())
                .category(dto.getCategory())
                .sku(dto.getSku())
                .imageUrl(dto.getImageUrl())
                .isActive(dto.getIsActive())
                .rating(0.0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Product saved = productRepository.save(product);

        return mapToResponse(saved);
    }

    private ProductResponseDTO mapToResponse(Product saved) {
        return null;
    }

    @Override
    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO dto) {
        return null;
    }

    @Override
    public List<ProductResponseDTO> getAllProducts() {
        return List.of();
    }

    @Override
    public ProductResponseDTO getProductById(Long id) {
        return null;
    }

    @Override
    public void deleteProduct(Long id) {

    }

    // ✅ Local mapping method (no external mapper)
  
}