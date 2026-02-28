package com.demo.order;

import com.demo.product.Product;
import com.demo.product.ProductService;
import com.demo.user.User;
import com.demo.user.UserService;
import com.impactanalyzer.annotation.TrackImpact;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Demo service that demonstrates @TrackImpact in a realistic scenario.
 *
 * placeOrder() touches:
 *   DB reads  → users, products (via JPA)
 *   DB writes → orders (insert), products (stock update)
 *   API calls → payment gateway, notification service, fraud check (mock)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository  orderRepo;
    private final UserService      userService;
    private final ProductService   productService;
    private final RestTemplate     restTemplate;

    // ── ① Full order flow — the main tracked method ────────────────────────

    /**
     * Places an order end-to-end.
     * @TrackImpact records every DB op and outbound HTTP call made below.
     */
    @TrackImpact(label = "placeOrder")
    @Transactional
    public Order placeOrder(OrderRequest req) {
        log.info("Placing order for user={} product={} qty={}",
                req.getUserId(), req.getProductId(), req.getQuantity());

        // ── Step 1: Validate user (DB read: users) ─────────────────────────
        User user = userService.getUser(req.getUserId());
        log.info("User validated: {} [{}]", user.getName(), user.getTier());

        // ── Step 2: Check product availability (DB read: products) ─────────
        Product product = productService.getProduct(req.getProductId());
        if (product.getStock() < req.getQuantity()) {
            throw new RuntimeException("Insufficient stock");
        }

        // ── Step 3: Check prior orders for this user (DB read: orders) ─────
        List<Order> priorOrders = orderRepo.findByUserId(req.getUserId());
        log.info("User has {} prior orders", priorOrders.size());

        // ── Step 4: External fraud check (API call) ─────────────────────────
        runFraudCheck(req.getUserId(), product.getPrice().multiply(
                BigDecimal.valueOf(req.getQuantity())));

        // ── Step 5: Reserve stock (DB update: products) ────────────────────
        productService.reserveStock(req.getProductId(), req.getQuantity());

        // ── Step 6: Persist the order (DB insert: orders) ──────────────────
        Order order = new Order();
        order.setUserId(req.getUserId());
        order.setProductId(req.getProductId());
        order.setQuantity(req.getQuantity());
        order.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(req.getQuantity())));
        order.setStatus("CONFIRMED");
        Order saved = orderRepo.save(order);

        // ── Step 7: Charge payment (API call) ──────────────────────────────
        chargePayment(saved.getId(), saved.getTotalPrice(), user);

        // ── Step 8: Send confirmation notification (API call) ──────────────
        sendNotification(user.getEmail(), saved.getId(), product.getName());

        log.info("Order {} confirmed. Total: {}", saved.getId(), saved.getTotalPrice());
        return saved;
    }

    // ── ② Fetch order summary — a lighter tracked method ──────────────────

    @TrackImpact(label = "getOrderSummary")
    @Transactional(readOnly = true)
    public Map<String, Object> getOrderSummary(Long userId) {
        User         user    = userService.getUser(userId);
        List<Order>  orders  = orderRepo.findByUserId(userId);
        List<Product> catalog = productService.getAvailableProducts();

        // External API: fetch shipping rates (API call)
        fetchShippingRates(userId);

        return Map.of(
                "user",      user.getName(),
                "tier",      user.getTier(),
                "orderCount", orders.size(),
                "catalogSize", catalog.size()
        );
    }

    // ── ③ Process pending orders — batch tracked method ───────────────────

    @TrackImpact(label = "processPendingOrders")
    @Transactional
    public int processPendingOrders() {
        List<Order> pending = orderRepo.findByStatus("PENDING");
        log.info("Processing {} pending orders", pending.size());

        int processed = 0;
        for (Order o : pending) {
            try {
                User    user    = userService.getUser(o.getUserId());
                Product product = productService.getProduct(o.getProductId());
                chargePayment(o.getId(), o.getTotalPrice(), user);
                sendNotification(user.getEmail(), o.getId(), product.getName());

                o.setStatus("CONFIRMED");
                orderRepo.save(o);
                processed++;
            } catch (Exception e) {
                log.warn("Failed to process order {}: {}", o.getId(), e.getMessage());
                o.setStatus("FAILED");
                orderRepo.save(o);
            }
        }
        return processed;
    }

    // ── Private helpers (each makes an outbound API call) ─────────────────

    private void runFraudCheck(Long userId, BigDecimal amount) {
        try {
            // Calls JSONPlaceholder as a mock fraud-check API
            String url = "https://jsonplaceholder.typicode.com/users/" + userId;
            restTemplate.getForObject(url, String.class);
            log.debug("Fraud check passed for user={}, amount={}", userId, amount);
        } catch (Exception e) {
            log.warn("Fraud check unavailable (offline?): {}", e.getMessage());
        }
    }

    private void chargePayment(Long orderId, BigDecimal amount, User user) {
        try {
            // Calls JSONPlaceholder as a mock payment gateway
            String url = "https://jsonplaceholder.typicode.com/posts/" + orderId;
            restTemplate.getForObject(url, String.class);
            log.debug("Payment charged: order={} amount={} user={}", orderId, amount, user.getEmail());
        } catch (Exception e) {
            log.warn("Payment gateway unavailable (offline?): {}", e.getMessage());
        }
    }

    private void sendNotification(String email, Long orderId, String productName) {
        try {
            // Calls JSONPlaceholder as a mock notification service
            String url = "https://jsonplaceholder.typicode.com/todos/" + orderId;
            restTemplate.getForObject(url, String.class);
            log.debug("Notification sent to {} for order {} ({})", email, orderId, productName);
        } catch (Exception e) {
            log.warn("Notification service unavailable (offline?): {}", e.getMessage());
        }
    }

    private void fetchShippingRates(Long userId) {
        try {
            String url = "https://jsonplaceholder.typicode.com/albums/" + userId;
            restTemplate.getForObject(url, String.class);
            log.debug("Shipping rates fetched for user={}", userId);
        } catch (Exception e) {
            log.warn("Shipping service unavailable (offline?): {}", e.getMessage());
        }
    }
}
