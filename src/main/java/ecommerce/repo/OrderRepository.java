package ecommerce.repo;

import ecommerce.entity.Order;
import ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);
    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);
    List<Order> findByUser(User user);

    @Query("SELECT DATE(o.orderDate), COUNT(o) FROM Order o GROUP BY DATE(o.orderDate)")
    List<Object[]> getOrdersPerDay();
}

