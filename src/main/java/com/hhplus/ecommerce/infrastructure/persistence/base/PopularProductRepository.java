package com.hhplus.ecommerce.infrastructure.persistence.base;

import com.hhplus.ecommerce.domain.product.PopularProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PopularProductRepository extends JpaRepository<PopularProduct, Long> {

    /**
     * 특정 기간 타입과 날짜로 인기 상품 조회
     */
    @Query("SELECT pp FROM PopularProduct pp " +
            "WHERE pp.periodType = :periodType " +
            "AND pp.aggregatedDate = :aggregatedDate " +
            "ORDER BY pp.salesCount DESC")
    List<PopularProduct> findByPeriodTypeAndAggregatedDate(
            @Param("periodType") PopularProduct.PeriodType periodType,
            @Param("aggregatedDate") LocalDate aggregatedDate);

    /**
     * 최근 N일간 일별 집계 데이터 조회
     */
    @Query("SELECT pp FROM PopularProduct pp " +
            "WHERE pp.periodType = :periodType " +
            "AND pp.aggregatedDate >= :startDate " +
            "ORDER BY pp.salesCount DESC")
    List<PopularProduct> findRecentByPeriodType(
            @Param("periodType") PopularProduct.PeriodType periodType,
            @Param("startDate") LocalDate startDate);

    /**
     * 특정 상품, 기간 타입, 날짜로 집계 데이터 조회
     */
    Optional<PopularProduct> findByProductIdAndPeriodTypeAndAggregatedDate(
            Long productId,
            PopularProduct.PeriodType periodType,
            LocalDate aggregatedDate);

    /**
     * 기간 타입별 상위 N개 조회
     */
    @Query("SELECT pp FROM PopularProduct pp " +
            "WHERE pp.periodType = :periodType " +
            "AND pp.aggregatedDate = :aggregatedDate " +
            "ORDER BY pp.salesCount DESC " +
            "LIMIT :limit")
    List<PopularProduct> findTopNByPeriodTypeAndDate(
            @Param("periodType") PopularProduct.PeriodType periodType,
            @Param("aggregatedDate") LocalDate aggregatedDate,
            @Param("limit") int limit);
}
