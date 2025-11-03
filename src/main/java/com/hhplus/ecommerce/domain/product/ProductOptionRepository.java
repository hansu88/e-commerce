package com.hhplus.ecommerce.domain.product;

import java.util.List;
import java.util.Optional;

public interface ProductOptionRepository {

    /**
     * 상품 옵션 저장 (초기 데이터 세팅용)
     */
    ProductOption save(ProductOption productOption);

    /**
     * ID로 옵션 조회 (재고 확인, 장바구니 담기용)
     */
    Optional<ProductOption> findById(Long id);

    /**
     * 특정 상품의 모든 옵션 조회 (GET /api/products/{id})
     */
    List<ProductOption> findByProductId(Long productId);

    /**
     * 모든 옵션 조회 (인기 상품 집계, 통계용)
     */
    List<ProductOption> findAll();
}
