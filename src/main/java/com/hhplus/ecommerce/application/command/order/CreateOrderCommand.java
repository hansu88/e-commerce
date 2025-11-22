package com.hhplus.ecommerce.application.command.order;

import com.hhplus.ecommerce.domain.order.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CreateOrderCommand {
    private final Long userId;
    private final List<OrderItem> orderItems;
    private final Long userCouponId;

    /**
     * Command 유효성 검증
     * @throws IllegalArgumentException 유효하지 않은 파라미터가 있을 경우
     */
    public void validate() {
        if (userId == null) throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        if (orderItems == null || orderItems.isEmpty()) throw new IllegalArgumentException("주문 항목은 최소 1개 이상이어야 합니다.");
        for (OrderItem item : orderItems) {
            if (item.getProductOptionId() == null) throw new IllegalArgumentException("상품 옵션 ID는 필수입니다.");
            if (item.getQuantity() <= 0) throw new IllegalArgumentException("주문 수량은 1개 이상이어야 합니다.");
        }
    }
}
