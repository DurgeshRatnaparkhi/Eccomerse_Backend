    package ecommerce.entity;

    import jakarta.persistence.*;
    import lombok.*;
    import java.math.BigDecimal;
    import java.time.LocalDateTime;

    @Entity
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class Product {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String name;
        private String description;
        private BigDecimal price;
        private Integer quantityAvailable;
        private String brand;
        private String category;

        private String sku; // Stock Keeping Unit, e.g., "APL-IP15-256GB"
        private Double rating;

        private String imageUrl; // Main product image (kept for backward compatibility)

        // ✅ Store product image as BLOB in database
        @Lob
        @Column(name = "image_data", columnDefinition = "LONGBLOB")
        private byte[] imageData;

        private Boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        // ✅ Relation with User
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id", nullable = false)
        private User user;



    }