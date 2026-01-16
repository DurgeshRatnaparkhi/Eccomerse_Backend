package ecommerce.controller;


import ecommerce.dtos.OrderResponseDTO;
import ecommerce.dtos.PlaceOrderRequestDTO;
import ecommerce.entity.User;
import ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/place")
    public ResponseEntity<OrderResponseDTO> placeOrder(@RequestBody PlaceOrderRequestDTO request,
                                                       @AuthenticationPrincipal User user){

        log.info("Place order API called for userId={}", user.getId());
        OrderResponseDTO response = orderService.placeOrder(user, request);

        return ResponseEntity.ok(response);

    }
}
