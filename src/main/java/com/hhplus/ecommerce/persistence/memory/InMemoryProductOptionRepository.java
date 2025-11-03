package com.hhplus.ecommerce.persistence.memory;

import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.domain.product.ProductOptionRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 상품 옵션 인메모리 Repository 구현
 */
@Repository
public class InMemoryProductOptionRepository implements ProductOptionRepository {

    // Thread-safe한 저장소
    private final Map<Long, ProductOption> store = new ConcurrentHashMap<>();

    // ID 자동 생성 (동시성 안전)
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public ProductOption save(ProductOption productOption) {
        // ID가 없으면 자동 생성
        if (productOption.getId() == null) {
            productOption.setId(idGenerator.getAndIncrement());
        }
        // Map에 저장
        store.put(productOption.getId(), productOption);
        return productOption;
    }

    @Override
    public Optional<ProductOption> findById(Long id) {
        // Map에서 ID로 찾기
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<ProductOption> findByProductId(Long productId) {
        // Map의 모든 값을 Stream으로 변환
        // → productId가 같은 것만 필터링
        // → List로 수집
        return store.values().stream()
                .filter(option -> option.getProductId().equals(productId))
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductOption> findAll() {
        // Map의 모든 값을 List로 변환
        return new ArrayList<>(store.values());
    }
}
