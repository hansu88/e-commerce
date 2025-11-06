package com.hhplus.ecommerce.infrastructure.persistence.memory;

import com.hhplus.ecommerce.domain.cart.Cart;
import com.hhplus.ecommerce.infrastructure.persistence.base.CartRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryCartRepository implements CartRepository {
    private final Map<Long, Cart> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Cart save(Cart cart) {
        if (cart.getId() == null) {
            cart.setId(idGenerator.incrementAndGet());
        }
        store.put(cart.getId(), cart);
        return cart;
    }

    @Override
    public Optional<Cart> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Cart> findByUserId(Long userId) {
        return store.values().stream()
                .filter(cart -> cart.getUserId().equals(userId))
                .findFirst();
    }

    @Override
    public List<Cart> findAll() {
        return new ArrayList<>(store.values());
    }
}
