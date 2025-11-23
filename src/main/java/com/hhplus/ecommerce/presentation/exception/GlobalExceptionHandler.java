package com.hhplus.ecommerce.presentation.exception;

import com.hhplus.ecommerce.presentation.dto.response.util.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 글로벌 예외 처리 핸들러
 *
 * 동시성 제어 관련 예외 처리:
 * - IllegalStateException: 쿠폰 한도 초과, 재시도 한도 초과 등 → 409 Conflict
 * - OutOfStockException: 재고 부족 → 409 Conflict
 * - OptimisticLockingFailureException: 낙관적 락 충돌 → 409 Conflict
 * - DataIntegrityViolationException: UNIQUE 제약 위반 등 → 409 Conflict
 *
 * 일반 예외 처리:
 * - IllegalArgumentException: 잘못된 요청 → 400 Bad Request
 * - ProductNotFoundException: 상품 없음 → 404 Not Found
 * - Exception: 기타 예외 → 500 Internal Server Error
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 잘못된 요청 (400 Bad Request)
     * - 유효하지 않은 파라미터
     * - 포인트 부족, 잘못된 수량 등
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("잘못된 요청: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
    }

    /**
     * 상품을 찾을 수 없음 (404 Not Found)
     */
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFoundException(ProductNotFoundException e) {
        log.warn("상품을 찾을 수 없음: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("PRODUCT_NOT_FOUND", e.getMessage()));
    }

    /**
     * 비즈니스 규칙 위반 (409 Conflict)
     * - 쿠폰 발급 한도 초과
     * - 재시도 한도 초과
     * - 이미 발급받은 쿠폰
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e) {
        log.warn("비즈니스 규칙 위반: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("CONFLICT", e.getMessage()));
    }

    /**
     * 재고 부족 (409 Conflict)
     */
    @ExceptionHandler(OutOfStockException.class)
    public ResponseEntity<ErrorResponse> handleOutOfStockException(OutOfStockException e) {
        log.warn("재고 부족: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("OUT_OF_STOCK", e.getMessage()));
    }

    /**
     * 낙관적 락 충돌 (409 Conflict)
     * - 동시에 같은 엔티티를 수정하려는 경우
     * - 재시도 로직에서 처리되지만, 최종 실패 시 발생
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailureException(OptimisticLockingFailureException e) {
        log.warn("동시성 충돌 (낙관적 락): {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("OPTIMISTIC_LOCK_FAILURE", "동시에 여러 요청이 처리되었습니다. 다시 시도해주세요."));
    }

    /**
     * 데이터 무결성 제약 위반 (409 Conflict)
     * - UNIQUE 제약 위반 (중복 데이터)
     * - Foreign Key 제약 위반
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.warn("데이터 무결성 제약 위반: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DATA_INTEGRITY_VIOLATION", "이미 존재하는 데이터이거나 제약 조건을 위반했습니다."));
    }

    /**
     * 기타 예외 (500 Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("서버 오류: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."));
    }
}
