package com.hhplus.ecommerce.application.service;

import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductOptionRepository;
import com.hhplus.ecommerce.domain.stock.StockHistory;
import com.hhplus.ecommerce.infrastructure.persistence.base.StockHistoryRepository;
import com.hhplus.ecommerce.presentation.exception.OutOfStockException;
import com.hhplus.ecommerce.domain.stock.StockChangeReason;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 재고 관리 서비스
 * - 재고 차감/증가
 * - 재고 검증
 * - 동시성 제어 적용 (ProductOption ID별 Lock)
 */
@Service
public class StockService {

    private final ProductOptionRepository productOptionRepository;
    private final StockHistoryRepository stockHistoryRepository;
    
    // ProductOption ID별 Lock 객체 관리
    private final Map<Long, Object> lockMap = new ConcurrentHashMap<>();

    public StockService(ProductOptionRepository productOptionRepository,
                       StockHistoryRepository stockHistoryRepository) {
        this.productOptionRepository = productOptionRepository;
        this.stockHistoryRepository = stockHistoryRepository;
    }

    /**
     * 재고 차감 (주문 시)
     * 동시성 제어: ProductOption ID별 synchronized 블록 적용
     * @param productOptionId 상품 옵션 ID
     * @param quantity 차감할 수량
     * @param reason 차감 사유
     */
    public void decreaseStock(Long productOptionId, int quantity, StockChangeReason reason) {
        // ProductOption ID별 Lock 객체 획득 (없으면 생성)
        Object lock = lockMap.computeIfAbsent(productOptionId, k -> new Object());
        
        synchronized (lock) {
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

            // StockHistory 기록 (음수로 저장)
            StockHistory history = new StockHistory(productOptionId, -quantity, reason);
            stockHistoryRepository.save(history);
        }
    }

    /**
     * 재고 증가 (주문 취소, 재입고)
     * 동시성 제어: ProductOption ID별 synchronized 블록 적용
     * @param productOptionId 상품 옵션 ID
     * @param quantity 증가할 수량
     * @param reason 증가 사유
     */
    public void increaseStock(Long productOptionId, int quantity, StockChangeReason reason) {
        // ProductOption ID별 Lock 객체 획득 (없으면 생성)
        Object lock = lockMap.computeIfAbsent(productOptionId, k -> new Object());
        
        synchronized (lock) {
            ProductOption option = productOptionRepository.findById(productOptionId)
                    .orElseThrow(() -> new IllegalArgumentException("상품 옵션을 찾을 수 없습니다: " + productOptionId));

            // 재고 증가
            option.setStock(option.getStock() + quantity);
            productOptionRepository.save(option);

            // StockHistory 기록 (양수로 저장)
            StockHistory history = new StockHistory(productOptionId, quantity, reason);
            stockHistoryRepository.save(history);
        }
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
