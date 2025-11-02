package com.hhplus.ecommerce.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "에러 응답")
public class ErrorResponse {
    @Schema(description = "에러 코드", example = "PRODUCT_NOT_FOUND")
    private String code;

    @Schema(description = "에러 메시지", example = "상품을 찾을 수 없습니다.")
    private String message;
}
