package ecommerce.dtos;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DashboardDTO {

    private long totalOrders;
    private double totalRevenue;
    private long totalUsers;
    private long totalProducts;
}