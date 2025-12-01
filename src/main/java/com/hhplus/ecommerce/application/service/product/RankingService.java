package com.hhplus.ecommerce.application.service.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 상품 랭킹 서비스
 * - Redis Sorted Set을 사용한 실시간 상품 랭킹
 * - 주문 시마다 점수(판매 수량) 증가
 * - TOP N 조회 가능
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Redis Sorted Set 키
     * - Key: "product:ranking"
     * - Score: 판매 수량 (누적)
     * - Member: 상품 ID (문자열)
     */
    private static final String RANKING_KEY = "product:ranking";

    /**
     * 상품 판매 시 점수 증가
     *
     * @param productId 상품 ID
     * @param quantity  판매 수량
     */
    public void incrementProductScore(Long productId, int quantity) {
        try {
            Double newScore = redisTemplate.opsForZSet()
                    .incrementScore(RANKING_KEY, productId.toString(), quantity);

            log.debug("상품 랭킹 업데이트 - 상품 ID: {}, 추가 수량: {}, 새 점수: {}",
                    productId, quantity, newScore);

        } catch (Exception e) {
            // Redis 장애 시 로깅만 하고 계속 진행 (랭킹은 부가 기능)
            log.error("상품 랭킹 업데이트 실패 - 상품 ID: {}, 수량: {}", productId, quantity, e);
        }
    }

    /**
     * TOP N 상품 ID 조회
     *
     * @param count 조회할 개수 (예: TOP 5)
     * @return 상품 ID 리스트 (판매량 높은 순)
     */
    public List<RankingItem> getTopProducts(int count) {
        try {
            // 0부터 count-1까지 조회 (예: TOP 5 = 0~4)
            Set<ZSetOperations.TypedTuple<String>> topSet =
                    redisTemplate.opsForZSet()
                            .reverseRangeWithScores(RANKING_KEY, 0, count - 1);

            if (topSet == null || topSet.isEmpty()) {
                log.debug("랭킹 데이터 없음");
                return Collections.emptyList();
            }

            // TypedTuple → RankingItem 변환
            int rank = 1; // 순위는 1부터 시작
            List<RankingItem> items = topSet.stream()
                    .map(tuple -> new RankingItem(
                            Long.parseLong(tuple.getValue()),           // 상품 ID
                            tuple.getScore().intValue(),                // 판매 수량
                            rank                                        // 순위는 외부에서 증가
                    ))
                    .collect(Collectors.toList());

            // 순위 부여 (1, 2, 3, ...)
            for (int i = 0; i < items.size(); i++) {
                items.get(i).setRank(i + 1);
            }

            log.debug("TOP {} 상품 조회 완료 - {} 개", count, items.size());
            return items;

        } catch (Exception e) {
            log.error("TOP {} 상품 조회 실패", count, e);
            return Collections.emptyList();
        }
    }

    /**
     * 특정 상품의 판매 수량(점수) 조회
     *
     * @param productId 상품 ID
     * @return 판매 수량 (없으면 0)
     */
    public Integer getProductScore(Long productId) {
        try {
            Double score = redisTemplate.opsForZSet()
                    .score(RANKING_KEY, productId.toString());

            return score != null ? score.intValue() : 0;

        } catch (Exception e) {
            log.error("상품 점수 조회 실패 - 상품 ID: {}", productId, e);
            return 0;
        }
    }

    /**
     * 특정 상품의 순위 조회
     *
     * @param productId 상품 ID
     * @return 순위 (1위, 2위, ...) / 없으면 null
     */
    public Integer getProductRank(Long productId) {
        try {
            Long rank = redisTemplate.opsForZSet()
                    .reverseRank(RANKING_KEY, productId.toString());

            // Redis rank는 0부터 시작 (0 = 1위)
            return rank != null ? rank.intValue() + 1 : null;

        } catch (Exception e) {
            log.error("상품 순위 조회 실패 - 상품 ID: {}", productId, e);
            return null;
        }
    }

    /**
     * 랭킹 초기화 (관리자 기능 / 스케줄러용)
     * - 매월 1일 초기화 등에 사용 가능
     */
    public void resetRanking() {
        try {
            redisTemplate.delete(RANKING_KEY);
            log.info("상품 랭킹 초기화 완료");

        } catch (Exception e) {
            log.error("상품 랭킹 초기화 실패", e);
        }
    }

    /**
     * 랭킹 아이템 (내부 DTO)
     * - Redis 조회 결과를 담는 간단한 객체
     */
    @lombok.Getter
    @lombok.Setter
    @lombok.AllArgsConstructor
    public static class RankingItem {
        private Long productId;      // 상품 ID
        private Integer soldCount;   // 판매 수량
        private Integer rank;        // 순위
    }
}
