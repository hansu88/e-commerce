package com.hhplus.ecommerce.infrastructure.persistence.memory;

import com.hhplus.ecommerce.domain.coupon.Coupon;
import com.hhplus.ecommerce.domain.coupon.CouponRepository;

import javax.swing.plaf.PanelUI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryCouponRepository implements CouponRepository {
    private final Map<Long, Coupon> coupons = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong();

    /**
     * 쿠폰 관련 저장
     * @param coupon
     */
    @Override
    public Coupon save(Coupon coupon) {
        if (coupon.getId() == null) {
            coupon.setId(counter.incrementAndGet());
        }
        coupons.put(coupon.getId(), coupon);
        return coupon;
    }

    /**
     * 쿠폰 존재하면 반환 없으면 빈값
     * @param id
     *
     */
    @Override
    public Optional<Coupon> findById(Long id) {
        return Optional.ofNullable(coupons.get(id));
    }

    /**
     * 
     *  쿠폰 전체리스트 확인
     */
    @Override
    public List<Coupon> findAll() {
        return new ArrayList<>(coupons.values());
    }
}
