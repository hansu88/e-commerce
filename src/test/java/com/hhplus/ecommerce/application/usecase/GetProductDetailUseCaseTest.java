package com.hhplus.ecommerce.application.usecase;

import com.hhplus.ecommerce.application.command.GetProductDetailCommand;
import com.hhplus.ecommerce.application.usecase.product.GetProductDetailUseCase;
import com.hhplus.ecommerce.infrastructure.persistence.memory.InMemoryOrderItemRepository;
import com.hhplus.ecommerce.infrastructure.persistence.memory.InMemoryProductOptionRepository;
import com.hhplus.ecommerce.infrastructure.persistence.memory.InMemoryProductRepository;
import com.hhplus.ecommerce.presentation.exception.ProductNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

public class GetProductDetailUseCaseTest {

    private GetProductDetailUseCase getProductDetailUseCase;
    private InMemoryProductRepository productRepository;
    private InMemoryProductOptionRepository productOptionRepository;
    private InMemoryOrderItemRepository orderItemRepository;

    @BeforeEach
    void setUp() {
        productRepository = new InMemoryProductRepository();
        productOptionRepository = new InMemoryProductOptionRepository();
        orderItemRepository = new InMemoryOrderItemRepository();
        getProductDetailUseCase = new GetProductDetailUseCase(productRepository, productOptionRepository);
    }

    @Test
    @DisplayName("상품 상세 조회 시 존재하지 않으면 ProductNotFoudException 발생 ")
    void getProductDetailNotFoundExceptio() {

        // Given - 존재하지 않는 상품 ID
        Long noProductId = 999L;

        // When & Then - 서비스 호출 시 발생
        GetProductDetailCommand command = new GetProductDetailCommand(noProductId);
        assertThatThrownBy(() -> getProductDetailUseCase.execute(command))
                .as("상품이 존재하지 않으면 ProductNotFoundException이 발생해야 한다")
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("Product not found");
    }
}
