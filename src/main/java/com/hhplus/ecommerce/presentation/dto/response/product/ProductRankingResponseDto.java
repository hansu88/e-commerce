package com.hhplus.ecommerce.presentation.dto.response.product;

import com.hhplus.ecommerce.domain.product.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 상품 랭킹 응답 DTO
 */
@Getter
@AllArgsConstructor
public class ProductRankingResponseDto {

    private Long productId;           // 상품 ID
    private String productName;       // 상품명
    private Integer price;            // 가격
    private Integer totalSoldCount;   // 총 판매 수량 (랭킹 점수)
    private Integer rank;             // 순위 (1위, 2위, ...)

    /**
     * Product 엔티티와 Redis 점수로 DTO 생성
     */
    public static ProductRankingResponseDto from(Product product, int soldCount, int rank) {
        return new ProductRankingResponseDto(
                product.getId(),
                product.getName(),
                product.getPrice(),
                soldCount,
                rank
        );
    }
}
