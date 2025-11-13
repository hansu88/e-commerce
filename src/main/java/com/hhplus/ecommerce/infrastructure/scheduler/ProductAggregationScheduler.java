package com.hhplus.ecommerce.infrastructure.scheduler;

import com.hhplus.ecommerce.application.usecase.product.AggregatePopularProductsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 상품 집계 스케줄러
 * - 인기 상품 일별/월별 집계
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ProductAggregationScheduler {

    private final AggregatePopularProductsUseCase aggregatePopularProductsUseCase;

    /**
     * 인기 상품 일별 집계
     * - 매일 새벽 2시에 전날 데이터 집계
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void aggregateDailyPopularProducts() {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            log.info("=== 인기 상품 일별 집계 스케줄러 시작 ===");
            aggregatePopularProductsUseCase.aggregateDaily(yesterday);
            log.info("=== 인기 상품 일별 집계 스케줄러 완료 ===");
        } catch (Exception e) {
            log.error("인기 상품 일별 집계 중 오류 발생", e);
        }
    }

    /**
     * 인기 상품 월별 집계
     * - 매월 1일 새벽 3시에 전월 데이터 집계
     */
    @Scheduled(cron = "0 0 3 1 * ?")
    public void aggregateMonthlyPopularProducts() {
        try {
            YearMonth lastMonth = YearMonth.now().minusMonths(1);
            log.info("=== 인기 상품 월별 집계 스케줄러 시작 ===");
            aggregatePopularProductsUseCase.aggregateMonthly(lastMonth);
            log.info("=== 인기 상품 월별 집계 스케줄러 완료 ===");
        } catch (Exception e) {
            log.error("인기 상품 월별 집계 중 오류 발생", e);
        }
    }
}
