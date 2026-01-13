package ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    private Double orderAmount;

    private String orderStatus;   // PENDING, CONFIRMED, SHIPPED, DELIVERED
    private String paymentStatus; // PENDING, SUCCESS, FAILED

    private String paymentId;     // Razorpay payment id

    private LocalDateTime orderDate;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems;
}
