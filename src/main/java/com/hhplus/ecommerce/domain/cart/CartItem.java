package com.hhplus.ecommerce.domain.cart;

/**
 * 장바구니 항목 Entity
 */
public class CartItem {
    private Long id;
    private Long cartId;
    private Long productOptionId;
    private Integer quantity;

    // 생성자
    public CartItem(Long id, Long cartId, Long productOptionId, Integer quantity) {
        this.id = id;
        this.cartId = cartId;
        this.productOptionId = productOptionId;
        this.quantity = quantity;
    }

    // Getter
    public Long getId() { return id; }
    public Long getCartId() { return cartId; }
    public Long getProductOptionId() { return productOptionId; }
    public Integer getQuantity() { return quantity; }

    // Setter
    public void setId(Long id) { this.id = id; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}