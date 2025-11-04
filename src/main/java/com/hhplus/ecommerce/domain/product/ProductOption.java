package com.hhplus.ecommerce.domain.product;

import com.hhplus.ecommerce.domain.stock.StockHistory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import java.util.List;

/**
 * 상품 옵션 Entity
 */
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class ProductOption {
    private Long id;
    private Long productId;
    private String color;
    private String size;
    private Integer stock;

    private List<StockHistory> stockHistories;

    public ProductOption(Long productId, String color, String size, Integer stock) {
        this.productId = productId;
        this.color = color;
        this.size = size;
        this.stock = stock;
    }

    // 재고 차감
    public void reduceStock(int quantity) {
        if (stock < quantity) {
            throw new IllegalStateException("재고 부족: 현재 재고 " + stock);
        }
        stock -= quantity;
    }

    // 재고 복원
    public void restoreStock(int quantity) {
        stock += quantity;
    }
}
