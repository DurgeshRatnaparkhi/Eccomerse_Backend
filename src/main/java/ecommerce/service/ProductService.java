package ecommerce.service;


import ecommerce.dtos.ProductDto;
import ecommerce.entity.Product;
import ecommerce.entity.User;
import ecommerce.exception.ProductNotFoundException;
import ecommerce.repo.UserRepository;
import ecommerce.security.CustomUserDetails;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final ecommerce.repo.ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;


    private static final String PRODUCT_NOT_FOUND_LOG = "Product not found with ID: {}";
    private static final String USER_NOT_FOUND_LOG = "User not found with ID: {}";

    @Value("${file.upload-dir:uploads/images}")
    private String uploadDir;


    @Transactional
    public ProductDto addProduct(ProductDto productDto, MultipartFile imageFile) {

        logger.info("Attempting to add product: {}", productDto.getName());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            logger.error("User is not authenticated");
            throw new RuntimeException("User not authenticated");
        }

        // ✅ CORRECT WAY
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("Authenticated user not found with email: {}", email);
                    return new RuntimeException("Authenticated user not found. Please login again.");
                });

        Product product = modelMapper.map(productDto, Product.class);
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
                logger.error("Image upload failed", e);
                throw new RuntimeException("Image upload failed");
            }
        }

        Product savedProduct = productRepository.save(product);
        logger.info("Product added successfully with ID: {}", savedProduct.getId());

        return modelMapper.map(savedProduct, ProductDto.class);
    }




    public ProductDto updateProduct(Long id, ProductDto productDto, MultipartFile imageFile) {
        logger.info("Attempting to update product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error(PRODUCT_NOT_FOUND_LOG, id);
                    return new RuntimeException("PRODUCT_NOT_FOUND_LOG");
                });

        modelMapper.map(productDto, product);
        product.setUpdatedAt(LocalDateTime.now());

        if (productDto.getUserId() != null) {
            User user = userRepository.findById(productDto.getUserId())
                    .orElseThrow(() -> {
                        logger.error(USER_NOT_FOUND_LOG, productDto.getUserId());
                        return new RuntimeException("User not found");
                    });
            product.setUser(user);
        }

        // Handle image update
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                // Update byte[] in DB
                product.setImageData(imageFile.getBytes());

                // Update file system image as well
                String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
                Path uploadPath = Paths.get(uploadDir);
                Files.createDirectories(uploadPath);
                Path filePath = uploadPath.resolve(fileName);
                imageFile.transferTo(filePath.toFile());
                product.setImageUrl("/images/" + fileName);
            } catch (IOException e) {
                logger.error("Failed to store updated image file", e);
                throw new RuntimeException("Image upload failed");
            }
        }

        Product updatedProduct = productRepository.save(product);
        logger.info("Product updated successfully with ID: {}", updatedProduct.getId());
        return modelMapper.map(updatedProduct, ProductDto.class);
    }


    @Transactional(readOnly = true)
    public List<ProductDto> getAllProducts() {
        logger.info("Fetching all products for admin");
        List<ProductDto> products = productRepository.findAll().stream()
                .map(product -> {
                    ProductDto dto = modelMapper.map(product, ProductDto.class);
                    dto.setImageData(product.getImageData()); // important
                    return dto;
                })

                .toList();
        logger.info("Retrieved {} products", products.size());
        return products;
    }

    @Transactional(readOnly = true)
    public ProductDto getProductById(Long id) {
        logger.info("Fetching product with ID: {} for admin", id);
//        checkAdminAccess();
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error(PRODUCT_NOT_FOUND_LOG, id);
                    return new RuntimeException("Product not found");
                });
        logger.info("Product retrieved successfully with ID: {}", id);
        return modelMapper.map(product, ProductDto.class);
    }

    @Transactional
    public void deleteProduct(Long id) {
        logger.info("Attempting to delete product with ID: {}", id);
//        checkAdminAccess();

        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error(PRODUCT_NOT_FOUND_LOG, id);
                    return new ProductNotFoundException(id);
                });

        // Delete image file if exists
        String imageUrl = product.getImageUrl();
        if (imageUrl != null && !imageUrl.isBlank()) {
            Path filePath = Paths.get(uploadDir, Paths.get(imageUrl).getFileName().toString());
            try {
                Files.deleteIfExists(filePath);
                logger.info("Deleted associated image file: {}", filePath);
            } catch (IOException e) {
                logger.warn("Failed to delete image file: {}", filePath, e);
            }
        }

        productRepository.deleteById(id);
        logger.info("Product deleted successfully with ID: {}", id);
    }


    //Fetch product image from DB as byte[]

    @Transactional(readOnly = true)
    public byte[] getProductImage(Long id) {
        logger.info("Fetching image for product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error(PRODUCT_NOT_FOUND_LOG, id);
                    return new ProductNotFoundException(id);
                });

        byte[] imageData = product.getImageData();

        if (imageData == null || imageData.length == 0) {
            logger.warn("No image found for product with ID: {}", id);
            return null;
        }

        logger.info("Image fetched successfully for product with ID: {}", id);
        return imageData;
    }
    @PostConstruct
    public void configureMapper() {
        modelMapper.typeMap(Product.class, ProductDto.class).addMappings(mapper -> {
            mapper.map(Product::getImageData, ProductDto::setImageData);
        });
    }

    public Page<ProductDto> searchProducts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Product> products = productRepository.search(
                keyword.toLowerCase(), pageable
        );

        return products.map(product -> {
            ProductDto dto = modelMapper.map(product, ProductDto.class);
            dto.setImageData(product.getImageData());
            return dto;
        });
    }


}
