package com.hhplus.ecommerce.infrastructure.persistence.memory;

import com.hhplus.ecommerce.domain.stock.StockHistory;
import com.hhplus.ecommerce.infrastructure.persistence.base.StockHistoryRepository;
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
public class InMemoryStockHistoryRepository implements StockHistoryRepository {

    private final Map<Long, StockHistory> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public StockHistory save(StockHistory stockHistory) {
        if (stockHistory.getId() == null) {
            // ID가 없으면 새로 생성
            StockHistory newHistory = new StockHistory(
                    idGenerator.incrementAndGet(),
                    stockHistory.getProductOptionId(),
                    stockHistory.getChangeQty(),
                    stockHistory.getReason(),
                    stockHistory.getCreatedAt() != null ? stockHistory.getCreatedAt() : LocalDateTime.now()
            );
            store.put(newHistory.getId(), newHistory);
            return newHistory;
        } else {
            store.put(stockHistory.getId(), stockHistory);
            return stockHistory;
        }
    }

    @Override
    public Optional<StockHistory> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<StockHistory> findByProductOptionId(Long productOptionId) {
        return store.values().stream()
                .filter(history -> history.getProductOptionId().equals(productOptionId))
                .collect(Collectors.toList());
    }

    @Override
    public List<StockHistory> findAll() {
        return new ArrayList<>(store.values());
    }
}
