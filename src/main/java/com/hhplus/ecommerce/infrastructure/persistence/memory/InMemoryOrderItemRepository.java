package com.hhplus.ecommerce.infrastructure.persistence.memory;

import com.hhplus.ecommerce.domain.order.OrderItem;
import com.hhplus.ecommerce.infrastructure.persistence.base.OrderItemRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryOrderItemRepository implements OrderItemRepository {

    private final Map<Long, OrderItem> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public OrderItem save(OrderItem orderItem) {
        if (orderItem.getId() == null) {
            orderItem.setId(idGenerator.incrementAndGet());
        }
        if (orderItem.getCreatedAt() == null) {
            orderItem.setCreatedAt(LocalDateTime.now());
        }
        store.put(orderItem.getId(), orderItem);
        return orderItem;
    }

    @Override
    public Optional<OrderItem> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<OrderItem> findByOrderId(Long orderId) {
        return store.values().stream()
                .filter(item -> item.getOrderId().equals(orderId))
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderItem> findAll() {
        return new ArrayList<>(store.values());
    }
}
