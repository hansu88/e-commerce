package com.hhplus.ecommerce.application.usecase.product;

import com.hhplus.ecommerce.application.command.product.GetProductRankingCommand;
import com.hhplus.ecommerce.application.service.product.RankingService;
import com.hhplus.ecommerce.domain.product.Product;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductRepository;
import com.hhplus.ecommerce.presentation.dto.response.product.ProductRankingResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 상품 랭킹 조회 UseCase
 * - Redis Sorted Set에서 TOP N 조회
 * - Product 정보와 조합하여 응답
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GetProductRankingUseCase {

    private final RankingService rankingService;
    private final ProductRepository productRepository;

    /**
     * 상품 랭킹 조회
     *
     * @param command 조회 조건 (count)
     * @return 상품 랭킹 리스트
     */
    @Transactional(readOnly = true)
    public List<ProductRankingResponseDto> execute(GetProductRankingCommand command) {
        command.validate();

        // 1. Redis에서 TOP N 상품 ID + 판매 수량 조회
        List<RankingService.RankingItem> topItems = rankingService.getTopProducts(command.getCount());

        if (topItems.isEmpty()) {
            log.info("랭킹 데이터 없음");
            return List.of();
        }

        // 2. Product ID 목록 추출
        List<Long> productIds = topItems.stream()
                .map(RankingService.RankingItem::getProductId)
                .toList();

        // 3. DB에서 Product 정보 조회 (IN 쿼리 1번)
        List<Product> products = productRepository.findAllById(productIds);

        // 4. Product ID를 키로 하는 Map 생성 (빠른 조회용)
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // 5. 랭킹 순서대로 DTO 생성
        List<ProductRankingResponseDto> result = new ArrayList<>();
        for (RankingService.RankingItem item : topItems) {
            Product product = productMap.get(item.getProductId());

            if (product != null) {
                result.add(ProductRankingResponseDto.from(
                        product,
                        item.getSoldCount(),
                        item.getRank()
                ));
            }
        }

        log.info("상품 랭킹 조회 완료 - TOP {}: {} 개", command.getCount(), result.size());
        return result;
    }
}
