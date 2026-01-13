package ecommerce.repo;

import ecommerce.entity.Cart;
import ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Cart findByUser(User user);
}

