package ecommerce.repo;

import ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository  extends JpaRepository<Product,Long> {

    boolean existsBySku(String sku);

    @Query("""
        SELECT p FROM Product p 
        WHERE LOWER(p.name) LIKE %:keyword%
        OR LOWER(p.brand) LIKE %:keyword%
        OR LOWER(p.category) LIKE %:keyword%
        OR LOWER(p.description) LIKE %:keyword%
        """)
    Page<Product> search(@Param("keyword") String keyword, Pageable pageable);

}
