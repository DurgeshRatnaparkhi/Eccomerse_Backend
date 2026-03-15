package ecommerce.service;

import ecommerce.dtos.ProductDto;
import ecommerce.entity.Product;
import ecommerce.entity.User;
import ecommerce.exception.ProductNotFoundException;
import ecommerce.repo.ProductRepository;
import ecommerce.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Value("${file.upload-dir:uploads/images}")
    private String uploadDir;

    // ===============================
    // ADD PRODUCT
    // ===============================

    @Transactional
    public ProductDto addProduct(ProductDto productDto, MultipartFile imageFile) {

        logger.info("Adding product: {}", productDto.getName());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = modelMapper.map(productDto, Product.class);

        // ✅ Ensure stock is set
        if(productDto.getStock() != null){
            product.setStock(productDto.getStock());
        }

        product.setUser(user);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        product.setIsActive(Boolean.TRUE.equals(productDto.getIsActive()));

        if (imageFile != null && !imageFile.isEmpty()) {
            try {

                product.setImageData(imageFile.getBytes());

                String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();

                Path uploadPath = Paths.get(uploadDir);
                Files.createDirectories(uploadPath);

                Path filePath = uploadPath.resolve(fileName);
                imageFile.transferTo(filePath.toFile());

                product.setImageUrl("/images/" + fileName);

            } catch (IOException e) {
                throw new RuntimeException("Image upload failed");
            }
        }

        Product saved = productRepository.save(product);

        return modelMapper.map(saved, ProductDto.class);
    }

    // ===============================
    // UPDATE PRODUCT
    // ===============================

    @Transactional
    public ProductDto updateProduct(Long id, ProductDto productDto, MultipartFile imageFile) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        modelMapper.map(productDto, product);

        // ✅ Update stock properly
        if(productDto.getStock() != null){
            product.setStock(productDto.getStock());
        }

        product.setUpdatedAt(LocalDateTime.now());

        if (imageFile != null && !imageFile.isEmpty()) {
            try {

                product.setImageData(imageFile.getBytes());

                String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();

                Path uploadPath = Paths.get(uploadDir);
                Files.createDirectories(uploadPath);

                Path filePath = uploadPath.resolve(fileName);
                imageFile.transferTo(filePath.toFile());

                product.setImageUrl("/images/" + fileName);

            } catch (IOException e) {
                throw new RuntimeException("Image upload failed");
            }
        }

        Product updated = productRepository.save(product);

        return modelMapper.map(updated, ProductDto.class);
    }

    // ===============================
    // GET ALL PRODUCTS
    // ===============================

    @Transactional(readOnly = true)
    public List<ProductDto> getAllProducts() {

        return productRepository.findAll()
                .stream()
                .map(product -> {
                    ProductDto dto = modelMapper.map(product, ProductDto.class);
                    dto.setImageData(product.getImageData());
                    return dto;
                })
                .toList();
    }

    // ===============================
    // GET PRODUCT BY ID
    // ===============================

    @Transactional(readOnly = true)
    public ProductDto getProductById(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        return modelMapper.map(product, ProductDto.class);
    }

    // ===============================
    // DELETE PRODUCT
    // ===============================

    @Transactional
    public void deleteProduct(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        productRepository.delete(product);
    }

    // ===============================
    // PRODUCT IMAGE
    // ===============================

    @Transactional(readOnly = true)
    public byte[] getProductImage(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        return product.getImageData();
    }

    // ===============================
    // SEARCH PRODUCTS
    // ===============================

    public Page<ProductDto> searchProducts(String keyword, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        Page<Product> products = productRepository.search(keyword.toLowerCase(), pageable);

        return products.map(product -> {
            ProductDto dto = modelMapper.map(product, ProductDto.class);
            dto.setImageData(product.getImageData());
            return dto;
        });
    }

    // ===============================
    // MODEL MAPPER CONFIG
    // ===============================

    @PostConstruct
    public void configureMapper() {

        modelMapper.typeMap(Product.class, ProductDto.class)
                .addMappings(mapper ->
                        mapper.map(Product::getImageData, ProductDto::setImageData));
    }
}