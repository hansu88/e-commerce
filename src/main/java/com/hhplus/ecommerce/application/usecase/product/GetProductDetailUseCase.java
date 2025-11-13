package com.hhplus.ecommerce.application.usecase.product;

import com.hhplus.ecommerce.application.command.product.GetProductDetailCommand;
import com.hhplus.ecommerce.domain.product.Product;
import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductOptionRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductRepository;
import com.hhplus.ecommerce.presentation.exception.ProductNotFoundException;
import com.hhplus.ecommerce.presentation.dto.response.product.ProductDetailResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 상품 상세 조회 UseCase (옵션 포함)
 */
@Component
@RequiredArgsConstructor
public class GetProductDetailUseCase {

    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;

    @Transactional(readOnly = true)
    public ProductDetailResponseDto execute(GetProductDetailCommand command) {
        Product product = productRepository.findById(command.getProductId())
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + command.getProductId()));

        List<ProductOption> options = productOptionRepository.findByProductId(command.getProductId());

        int totalStock = options.stream()
                .mapToInt(ProductOption::getStock)
                .sum();

        List<ProductDetailResponseDto.ProductOptionDto> optionDtos = options.stream()
                .map(option -> new ProductDetailResponseDto.ProductOptionDto(
                        option.getId(),
                        option.getColor(),
                        option.getSize(),
                        option.getStock()
                ))
                .collect(Collectors.toList());

        return new ProductDetailResponseDto(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStatus().name(),
                totalStock,
                optionDtos
        );
    }
}
