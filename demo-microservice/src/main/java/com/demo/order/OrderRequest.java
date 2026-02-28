package com.demo.order;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class OrderRequest {
    private Long userId;
    private Long productId;
    private int  quantity;
}

// ── Response ──────────────────────────────────────────────────────────────────

// Defined as a separate top-level class but kept in same file for brevity
class OrderResponse {
    public Long        orderId;
    public String      status;
    public BigDecimal  totalPrice;
    public String      userName;
    public String      productName;
    public Instant     createdAt;
}
