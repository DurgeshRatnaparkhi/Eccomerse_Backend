package ecommerce.service;

import ecommerce.dtos.DashboardDTO;

import java.util.Map;

public interface DashboardService {

    DashboardDTO getDashboardStatus();

    Map<String, Long> getOrdersPerDay();
}
