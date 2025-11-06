package com.hhplus.ecommerce.infrastructure.persistence.memory;

import com.hhplus.ecommerce.domain.cart.CartItem;
import com.hhplus.ecommerce.infrastructure.persistence.base.CartItemRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryCartItemRepository implements CartItemRepository {
    private final Map<Long, CartItem> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public CartItem save(CartItem cartItem) {
        if (cartItem.getId() == null) {
            cartItem.setId(idGenerator.incrementAndGet());
        }
        store.put(cartItem.getId(), cartItem);
        return cartItem;
    }

    @Override
    public Optional<CartItem> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<CartItem> findByCartId(Long cartId) {
        return store.values().stream()
                .filter(item -> item.getCartId().equals(cartId))
                .collect(Collectors.toList());
    }

    @Override
    public List<CartItem> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void delete(CartItem cartItem) {
        store.remove(cartItem.getId());
    }
}
