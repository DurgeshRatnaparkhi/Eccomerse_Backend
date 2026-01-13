package ecommerce.controller;

import ecommerce.dtos.ProductDto;
import ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/products")
@RequiredArgsConstructor
public class ProductUserController {

    private final ProductService productService;

    // ✔ Get all products for user dashboard
    @GetMapping("/all")
    public ResponseEntity<List<ProductDto>> getAllProductsForUser() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    // ✔ Get product by ID
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    // ✔ Public image API (user can load images)
    @GetMapping("/image/{id}")
    public ResponseEntity<byte[]> getProductImage(@PathVariable Long id) {

        ProductDto product = productService.getProductById(id);

        if (product == null || product.getImageData() == null) {
            return ResponseEntity.notFound().build();
        }

        String fileName = product.getImageUrl();
        String extension = "";

        if (fileName != null && fileName.contains(".")) {
            extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }

        MediaType mediaType = switch (extension) {
            case "png" -> MediaType.IMAGE_PNG;
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "webp" -> MediaType.parseMediaType("image/webp");
            default -> MediaType.IMAGE_JPEG;
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);

        return new ResponseEntity<>(product.getImageData(), headers, HttpStatus.OK);
    }
}
