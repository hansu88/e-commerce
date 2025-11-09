package com.hhplus.ecommerce.application.usecase.stock;

import com.hhplus.ecommerce.application.command.ValidateStockCommand;
import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductOptionRepository;
import com.hhplus.ecommerce.presentation.exception.OutOfStockException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 재고 검증 UseCase (차감하지 않음)
 */
@Component
@RequiredArgsConstructor
public class ValidateStockUseCase {

    private final ProductOptionRepository productOptionRepository;

    public void execute(ValidateStockCommand command) {
        ProductOption option = productOptionRepository.findById(command.getProductOptionId())
                .orElseThrow(() -> new IllegalArgumentException("상품 옵션을 찾을 수 없습니다: " + command.getProductOptionId()));

        if (option.getStock() < command.getQuantity()) {
            throw new OutOfStockException(
                    String.format("재고 부족: %s %s (요청: %d, 재고: %d)",
                            option.getColor(), option.getSize(), command.getQuantity(), option.getStock())
            );
        }
    }
}
