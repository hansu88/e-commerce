package com.hhplus.ecommerce.domain.product;

/**
 * 상품 옵션 Entity
 */
public class ProductOption {
    private Long id;
    private Long productId;
    private String color;
    private String size;
    private Integer stock;

    // 생성자
    public ProductOption(Long id, Long productId, String color, String size, Integer stock) {
        this.id = id;
        this.productId = productId;
        this.color = color;
        this.size = size;
        this.stock = stock;
    }

    // Getter
    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getColor() { return color; }
    public String getSize() { return size; }
    public Integer getStock() { return stock; }

    // Setter
    public void setId(Long id) { this.id = id; }
    public void setStock(Integer stock) { this.stock = stock; }
}
