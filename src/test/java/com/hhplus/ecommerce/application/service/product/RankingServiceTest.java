package com.hhplus.ecommerce.application.service.product;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RankingService 통합 테스트
 * - Redis Testcontainers를 사용한 독립적인 테스트 환경
 * - 실제 Redis에서 Sorted Set 동작 검증
 */
@SpringBootTest
@Testcontainers
@DisplayName("RankingService 통합 테스트")
class RankingServiceTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private RankingService rankingService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 랭킹 초기화
        rankingService.resetRanking();
    }

    @AfterEach
    void tearDown() {
        // 테스트 후 정리
        rankingService.resetRanking();
    }

    @Test
    @DisplayName("상품 판매 시 점수가 증가한다")
    void incrementProductScore() {
        // given
        Long productId = 1L;
        int quantity = 10;

        // when
        rankingService.incrementProductScore(productId, quantity);

        // then
        Integer score = rankingService.getProductScore(productId);
        assertThat(score).isEqualTo(10);
    }

    @Test
    @DisplayName("여러 번 판매 시 점수가 누적된다")
    void incrementProductScore_Multiple() {
        // given
        Long productId = 1L;

        // when
        rankingService.incrementProductScore(productId, 10);
        rankingService.incrementProductScore(productId, 20);
        rankingService.incrementProductScore(productId, 15);

        // then
        Integer score = rankingService.getProductScore(productId);
        assertThat(score).isEqualTo(45); // 10 + 20 + 15
    }

    @Test
    @DisplayName("TOP 5 상품을 조회한다")
    void getTopProducts() {
        // given - 6개 상품 추가 (점수 다르게)
        rankingService.incrementProductScore(1L, 100);
        rankingService.incrementProductScore(2L, 80);
        rankingService.incrementProductScore(3L, 120);
        rankingService.incrementProductScore(4L, 50);
        rankingService.incrementProductScore(5L, 90);
        rankingService.incrementProductScore(6L, 30);

        // when - TOP 5 조회
        List<RankingService.RankingItem> top5 = rankingService.getTopProducts(5);

        // then
        assertThat(top5).hasSize(5);
        assertThat(top5.get(0).getProductId()).isEqualTo(3L); // 120점 - 1위
        assertThat(top5.get(0).getSoldCount()).isEqualTo(120);
        assertThat(top5.get(0).getRank()).isEqualTo(1);

        assertThat(top5.get(1).getProductId()).isEqualTo(1L); // 100점 - 2위
        assertThat(top5.get(1).getRank()).isEqualTo(2);

        assertThat(top5.get(2).getProductId()).isEqualTo(5L); // 90점 - 3위
        assertThat(top5.get(2).getRank()).isEqualTo(3);

        assertThat(top5.get(3).getProductId()).isEqualTo(2L); // 80점 - 4위
        assertThat(top5.get(3).getRank()).isEqualTo(4);

        assertThat(top5.get(4).getProductId()).isEqualTo(4L); // 50점 - 5위
        assertThat(top5.get(4).getRank()).isEqualTo(5);
    }

    @Test
    @DisplayName("특정 상품의 순위를 조회한다")
    void getProductRank() {
        // given
        rankingService.incrementProductScore(1L, 100);
        rankingService.incrementProductScore(2L, 80);
        rankingService.incrementProductScore(3L, 120);

        // when
        Integer rank1 = rankingService.getProductRank(1L);
        Integer rank2 = rankingService.getProductRank(2L);
        Integer rank3 = rankingService.getProductRank(3L);

        // then
        assertThat(rank3).isEqualTo(1); // 120점 - 1위
        assertThat(rank1).isEqualTo(2); // 100점 - 2위
        assertThat(rank2).isEqualTo(3); // 80점 - 3위
    }

    @Test
    @DisplayName("랭킹에 없는 상품의 점수는 0이다")
    void getProductScore_NotExists() {
        // when
        Integer score = rankingService.getProductScore(999L);

        // then
        assertThat(score).isEqualTo(0);
    }

    @Test
    @DisplayName("랭킹에 없는 상품의 순위는 null이다")
    void getProductRank_NotExists() {
        // when
        Integer rank = rankingService.getProductRank(999L);

        // then
        assertThat(rank).isNull();
    }

    @Test
    @DisplayName("랭킹을 초기화한다")
    void resetRanking() {
        // given
        rankingService.incrementProductScore(1L, 100);
        rankingService.incrementProductScore(2L, 80);

        // when
        rankingService.resetRanking();

        // then
        List<RankingService.RankingItem> top5 = rankingService.getTopProducts(5);
        assertThat(top5).isEmpty();
    }

    @Test
    @DisplayName("랭킹이 비어있을 때 TOP N 조회 시 빈 리스트를 반환한다")
    void getTopProducts_Empty() {
        // when
        List<RankingService.RankingItem> top5 = rankingService.getTopProducts(5);

        // then
        assertThat(top5).isEmpty();
    }
}
