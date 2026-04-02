package ecommerce.service.impl;

import ecommerce.dtos.DashboardDTO;
import ecommerce.repo.OrderRepository;
import ecommerce.repo.ProductRepository;
import ecommerce.repo.UserRepository;
import ecommerce.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;


    @Override
    public DashboardDTO getDashboardStatus() {
            long totalOrders = orderRepository.count();
            long totalUsers = userRepository.count();
            long totalProducts = productRepository.count();

            double totalRevenue = orderRepository.findAll()
                    .stream()
                    .mapToDouble(o -> o.getTotalAmount().doubleValue())
                    .sum();

            return DashboardDTO.builder()
                    .totalOrders(totalOrders)
                    .totalUsers(totalUsers)
                    .totalProducts(totalProducts)
                    .totalRevenue(totalRevenue)
                    .build();
        }

    public Map<String, Long> getOrdersPerDay() {

        List<Object[]> results = orderRepository.getOrdersPerDay();

        Map<String, Long> data = new LinkedHashMap<>();

        for (Object[] row : results) {
            String date = row[0].toString();
            Long count = (Long) row[1];
            data.put(date, count);
        }

        return data;
    }

    }