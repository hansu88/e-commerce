package com.hhplus.ecommerce.domain.order;

/**
 * 주문 항목 Entity
 */
public class OrderItem {
    private Long id;
    private Long orderId;
    private Long productOptionId;
    private Integer quantity;
    private Integer price;  // 주문 당시 가격 (나중에 상품 가격이 변해도 주문 내역은 유지)

    // 생성자
    public OrderItem(Long id, Long orderId, Long productOptionId, Integer quantity, Integer price) {
        this.id = id;
        this.orderId = orderId;
        this.productOptionId = productOptionId;
        this.quantity = quantity;
        this.price = price;
    }

    // Getter
    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public Long getProductOptionId() { return productOptionId; }
    public Integer getQuantity() { return quantity; }
    public Integer getPrice() { return price; }

    // Setter
    public void setId(Long id) { this.id = id; }
}