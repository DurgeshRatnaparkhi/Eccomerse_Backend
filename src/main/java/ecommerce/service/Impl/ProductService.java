package ecommerce.service.Impl;

import ecommerce.dtos.ProductRequestDTO;
import ecommerce.dtos.ProductResponseDTO;

import java.util.List;

public interface ProductService {


    ProductResponseDTO addProduct(ProductRequestDTO dto);

    ProductResponseDTO updateProduct(Long id, ProductRequestDTO dto);

    List<ProductResponseDTO> getAllProducts();

    ProductResponseDTO getProductById(Long id);

    void deleteProduct(Long id);
}
