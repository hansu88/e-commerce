package com.hhplus.ecommerce.infrastructure.persistence.memory;

import com.hhplus.ecommerce.domain.product.Product;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryProductRepository implements ProductRepository {

    private final Map<Long, Product> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Product save(Product product) {
        if (product.getId() == null) {
            // 새로운 Product - ID 자동 생성
            product.setId(idGenerator.getAndIncrement());
        }
        // 저장 (신규 또는 업데이트)
        store.put(product.getId(), product);
        return product;
    }

    @Override
    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Product> findAll() {
        return new ArrayList<>(store.values());
    }
}
