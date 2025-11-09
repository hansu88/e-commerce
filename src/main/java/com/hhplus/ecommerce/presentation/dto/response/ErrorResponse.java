package com.hhplus.ecommerce.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "에러 응답")
public record ErrorResponse(
    @Schema(description = "에러 코드", example = "PRODUCT_NOT_FOUND")
    String code,

    @Schema(description = "에러 메시지", example = "상품을 찾을 수 없습니다.")
    String message
) {}
