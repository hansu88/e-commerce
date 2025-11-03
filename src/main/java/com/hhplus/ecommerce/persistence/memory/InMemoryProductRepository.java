package com.hhplus.ecommerce.persistence.memory;

import com.hhplus.ecommerce.domain.product.Product;
import com.hhplus.ecommerce.domain.product.ProductRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 상품 인메모리 Repository 구현
 */
@Repository
public class InMemoryProductRepository implements ProductRepository {

    // Thread-safe한 저장소
    private final Map<Long, Product> store = new ConcurrentHashMap<>();

    // ID 자동 생성 (동시성 안전)
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Product save(Product product) {
        // ID가 없으면 자동 생성
        if (product.getId() == null) {
            product.setId(idGenerator.getAndIncrement());
        }
        // Map에 저장
        store.put(product.getId(), product);
        return product;
    }

    @Override
    public Optional<Product> findById(Long id) {
        // Map에서 ID로 찾기
        // 없으면 Optional.empty() 반환
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Product> findAll() {
        // Map의 모든 값을 List로 변환
        return new ArrayList<>(store.values());
    }
}