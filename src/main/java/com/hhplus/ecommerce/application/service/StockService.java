package com.hhplus.ecommerce.application.service;

import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.domain.product.ProductOptionRepository;
import com.hhplus.ecommerce.domain.stock.OutOfStockException;
import com.hhplus.ecommerce.domain.stock.StockChangeReason;
import org.springframework.stereotype.Service;

/**
 * 재고 관리 서비스
 * - 재고 차감/증가
 * - 재고 검증
 */
@Service
public class StockService {

    private final ProductOptionRepository productOptionRepository;

    public StockService(ProductOptionRepository productOptionRepository) {
        this.productOptionRepository = productOptionRepository;
    }

    /**
     * 재고 차감 (주문 시)
     * @param productOptionId 상품 옵션 ID
     * @param quantity 차감할 수량
     * @param reason 차감 사유
     * @throws IllegalArgumentException 옵션이 존재하지 않을 때
     * @throws OutOfStockException 재고가 부족할 때
     */
    public void decreaseStock(Long productOptionId, int quantity, StockChangeReason reason) {
        ProductOption option = productOptionRepository.findById(productOptionId)
                .orElseThrow(() -> new IllegalArgumentException("상품 옵션을 찾을 수 없습니다: " + productOptionId));

        // 재고 부족 검증
        if (option.getStock() < quantity) {
            throw new OutOfStockException(
                    String.format("재고 부족: %s %s (요청: %d, 재고: %d)",
                            option.getColor(), option.getSize(), quantity, option.getStock())
            );
        }

        // 재고 차감
        option.setStock(option.getStock() - quantity);
        productOptionRepository.save(option);

        // TODO: StockHistory 기록 (나중에 구현)
    }

    /**
     * 재고 증가 (주문 취소, 재입고)
     * @param productOptionId 상품 옵션 ID
     * @param quantity 증가할 수량
     * @param reason 증가 사유
     */
    public void increaseStock(Long productOptionId, int quantity, StockChangeReason reason) {
        ProductOption option = productOptionRepository.findById(productOptionId)
                .orElseThrow(() -> new IllegalArgumentException("상품 옵션을 찾을 수 없습니다: " + productOptionId));

        // 재고 증가
        option.setStock(option.getStock() + quantity);
        productOptionRepository.save(option);

        // TODO: StockHistory 기록 (나중에 구현)
    }

    /**
     * 재고 검증만 (차감하지 않음)
     * @param productOptionId 상품 옵션 ID
     * @param quantity 필요한 수량
     * @throws OutOfStockException 재고가 부족할 때
     */
    public void validateStock(Long productOptionId, int quantity) {
        ProductOption option = productOptionRepository.findById(productOptionId)
                .orElseThrow(() -> new IllegalArgumentException("상품 옵션을 찾을 수 없습니다: " + productOptionId));

        if (option.getStock() < quantity) {
            throw new OutOfStockException(
                    String.format("재고 부족: %s %s (요청: %d, 재고: %d)",
                            option.getColor(), option.getSize(), quantity, option.getStock())
            );
        }
    }
}
