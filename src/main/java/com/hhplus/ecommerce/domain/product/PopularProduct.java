package com.hhplus.ecommerce.domain.product;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 인기 상품 집계 엔티티
 * - 일별/월별 판매량 집계 데이터 저장
 * - 스케줄러를 통해 주기적으로 집계
 */
@Entity
@Table(name = "popular_products",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_period_date",
                        columnNames = {"product_id", "period_type", "aggregated_date"}
                )
        },
        indexes = {
                @Index(name = "idx_period_sales", columnList = "period_type,sales_count DESC"),
                @Index(name = "idx_aggregated_date", columnList = "aggregated_date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PopularProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 20)
    private PeriodType periodType;

    @Column(name = "sales_count", nullable = false)
    private Integer salesCount;

    @Column(name = "aggregated_date", nullable = false)
    private LocalDate aggregatedDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public PopularProduct(Long productId, PeriodType periodType, Integer salesCount, LocalDate aggregatedDate) {
        this.productId = productId;
        this.periodType = periodType;
        this.salesCount = salesCount;
        this.aggregatedDate = aggregatedDate;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateSalesCount(Integer salesCount) {
        this.salesCount = salesCount;
        this.updatedAt = LocalDateTime.now();
    }

    public enum PeriodType {
        DAILY,   // 일별 집계
        MONTHLY  // 월별 집계
    }
}
