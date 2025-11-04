package com.hhplus.ecommerce.infrastructure.persistence.memory;

import com.hhplus.ecommerce.domain.coupon.UserCoupon;
import com.hhplus.ecommerce.domain.coupon.UserCouponRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryUserCouponRepository implements UserCouponRepository {
    private final Map<Long, UserCoupon> store = new ConcurrentHashMap<>();
    private final AtomicLong userCounter = new AtomicLong();

    /**
     * 개인이 쿠폰 발급할 때 사용
     * @param userCoupon
     */
    @Override
    public  UserCoupon save(UserCoupon userCoupon) {
        if (userCoupon.getId() == null) {
            userCoupon.setId(userCounter.incrementAndGet());
        }
        store.put(userCoupon.getId(), userCoupon);
        return userCoupon;
    }

    /**
     * 자신의 쿠폰목록에서 번호 클릭시 존재하는지 여부
     * @param id
     */
    @Override
    public Optional<UserCoupon> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    /**
     * 쿠폰목록 전체 리스트
     * @return
     */
    @Override
    public List<UserCoupon> findAll() {
        return new ArrayList<>(store.values());
    }
}
