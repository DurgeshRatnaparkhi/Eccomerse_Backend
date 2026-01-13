    package ecommerce.controller;


    import ecommerce.dtos.ProductDto;
    import ecommerce.entity.Product;
    import ecommerce.service.ProductService;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.http.HttpHeaders;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.MediaType;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.*;
    import org.springframework.web.multipart.MultipartFile;

    import java.io.IOException;
    import java.util.List;

    @RestController
    @RequestMapping("api/admin/products")
    @RequiredArgsConstructor
    @Slf4j
    public class ProductController {

        private final ProductService productService;

        @PostMapping(value = "/addProducts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<ProductDto> addProduct(
                @RequestPart("product") ProductDto productDto,
                @RequestPart(value = "image", required = false) MultipartFile imageFile) {

            ProductDto savedProduct = productService.addProduct(productDto, imageFile);
            return ResponseEntity.ok(savedProduct);
        }


        @PutMapping(value = "/updateProduct/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        //    @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<ProductDto> updateProduct(
                @PathVariable Long id,
                @RequestPart("product") ProductDto productDto,
                @RequestPart(value = "image", required = false) MultipartFile imageFile) {

            log.info("Received request to update product with ID: {}", id);

            try {
                ProductDto updatedProduct = productService.updateProduct(id, productDto, imageFile);
                log.info("Product updated successfully with ID: {}", updatedProduct.getId());
                return ResponseEntity.ok(updatedProduct);
            } catch (Exception e) {
                log.error("Error while updating product :{}", e.getMessage(), e);
                throw e;
            }
        }

        @GetMapping("/getAllProducts")
        //    @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<List<ProductDto>> getAllProducts() {
            log.info("Received request to fetch all products for admin");

            try {
                List<ProductDto> products = productService.getAllProducts();
                log.info("Returning {} products", products.size());
                return ResponseEntity.ok(products);
            } catch (Exception e) {
                log.error("Error while fetching all products :{}", e.getMessage(), e);
                throw e;
            }
        }

        @GetMapping("/getProductById/{id}")
        //    @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
            log.info("Received request to fetch product with ID: {} for admin", id);

            try {
                ProductDto product = productService.getProductById(id);
                log.info("Product retrieved successfully with ID: {}", id);
                return ResponseEntity.ok(product);
            } catch (Exception e) {
                log.error("Error while fetching product with ID {}: {}", id, e.getMessage(), e);
                throw e;
            }
        }

        @DeleteMapping("/deleteProduct/{id}")
        //    @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
            log.info("Received request to delete product with ID: {}", id);

            try {
                productService.deleteProduct(id);
                log.info("Product deleted successfully with ID: {}", id);
                return ResponseEntity.noContent().build();
            } catch (Exception e) {
                log.error("Error while deleting product with ID {}: {}", id, e.getMessage(), e);
                throw e;
            }
        }

        //New endpoint to fetch product image directly as byte[] from DB
        @GetMapping("/getProductImage/{id}")
        public ResponseEntity<byte[]> getProductImage(@PathVariable Long id) throws IOException {
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

        @GetMapping("/search")
        public ResponseEntity<?> searchProducts(
                @RequestParam(defaultValue = "") String keyword,
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "5") int size
        ) {
            return ResponseEntity.ok(productService.searchProducts(keyword, page, size));
        }





    }
