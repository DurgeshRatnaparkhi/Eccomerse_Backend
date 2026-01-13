package ecommerce.exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long id) {
        super("product not found with " +id);
    }

}
