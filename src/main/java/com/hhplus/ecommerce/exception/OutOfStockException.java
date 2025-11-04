package com.hhplus.ecommerce.exception;

/**
 * 재고 부족 예외
 */
public class OutOfStockException extends RuntimeException {
    public OutOfStockException(String message) {
        super(message);
    }
}
