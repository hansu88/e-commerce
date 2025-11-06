package com.hhplus.ecommerce.infrastructure.persistence.memory;

import com.hhplus.ecommerce.domain.coupon.Coupon;
import com.hhplus.ecommerce.infrastructure.persistence.base.CouponRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryCouponRepository implements CouponRepository {
    private final Map<Long, Coupon> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong();

    @Override
    public Coupon save(Coupon coupon) {
        if (coupon.getId() == null) {
            coupon.setId(idGenerator.incrementAndGet());
        }
        store.put(coupon.getId(), coupon);
        return coupon;
    }

    @Override
    public Optional<Coupon> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Coupon> findAll() {
        return new ArrayList<>(store.values());
    }
}
