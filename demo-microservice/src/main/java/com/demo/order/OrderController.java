package com.demo.order;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService  orderService;
    private final OrderRepository orderRepo;

    /**
     * Place an order — triggers the main @TrackImpact flow.
     * POST /orders
     * Body: { "userId": 1, "productId": 2, "quantity": 1 }
     */
    @PostMapping
    public ResponseEntity<Order> placeOrder(@RequestBody OrderRequest req) {
        Order order = orderService.placeOrder(req);
        return ResponseEntity.ok(order);
    }

    /**
     * Get user order summary — triggers the lighter @TrackImpact flow.
     * GET /orders/summary/1
     */
    @GetMapping("/summary/{userId}")
    public Map<String, Object> getOrderSummary(@PathVariable Long userId) {
        return orderService.getOrderSummary(userId);
    }

    /**
     * Process all pending orders — triggers the batch @TrackImpact flow.
     * POST /orders/process-pending
     */
    @PostMapping("/process-pending")
    public Map<String, Integer> processPending() {
        int count = orderService.processPendingOrders();
        return Map.of("processed", count);
    }

    /**
     * Get all orders.
     * GET /orders
     */
    @GetMapping
    public List<Order> getAllOrders() {
        return orderRepo.findAll();
    }
}
