package com.hhplus.ecommerce.infrastructure.persistence.memory;

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
 * ProductOption 인메모리 저장소 구현체
 * ConcurrentHashMap을 사용하여 Thread-safe 보장
 */
@Repository
public class InMemoryProductOptionRepository implements ProductOptionRepository {

    private final Map<Long, ProductOption> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public ProductOption save(ProductOption productOption) {
        if (productOption.getId() == null) {
            // 새로운 ProductOption - ID 자동 생성
            productOption.setId(idGenerator.getAndIncrement());
        }
        // 저장 (신규 또는 업데이트)
        store.put(productOption.getId(), productOption);
        return productOption;
    }

    @Override
    public Optional<ProductOption> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<ProductOption> findByProductId(Long productId) {
        // Stream으로 필터링: 특정 Product에 속하는 옵션들만 조회
        return store.values().stream()
                .filter(option -> option.getProductId().equals(productId))
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductOption> findAll() {
        return new ArrayList<>(store.values());
    }
}
