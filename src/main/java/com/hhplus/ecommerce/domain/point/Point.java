package com.hhplus.ecommerce.domain.point;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 포인트 엔티티 (캡슐화 개선)
 * - userId별 포인트 잔액 관리
 * - @Version으로 동시성 제어
 * - Setter 제거, Builder 패턴 적용
 * - 비즈니스 로직 메서드로 캡슐화
 */
@Entity
@Table(
        name = "points",
        indexes = @Index(name = "idx_user_id", columnList = "user_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Point {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private Integer balance;  // 포인트 잔액

    @Version
    private Long version;  // 낙관적 락 (동시성 제어)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Point(Long userId, Integer initialBalance) {
        this.userId = userId;
        this.balance = initialBalance;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) this.createdAt = now;
        if (this.updatedAt == null) this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 포인트 적립
     * @param amount 적립할 포인트
     * @throws IllegalArgumentException 적립 금액이 0 이하인 경우
     */
    public void earn(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("적립 금액은 양수여야 합니다");
        }
        this.balance += amount;
    }

    /**
     * 포인트 사용
     * @param amount 사용할 포인트
     * @throws IllegalArgumentException 사용 금액이 0 이하이거나 잔액이 부족한 경우
     */
    public void use(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("사용 금액은 양수여야 합니다");
        }
        if (this.balance < amount) {
            throw new IllegalArgumentException("포인트가 부족합니다. 현재 잔액: " + this.balance + ", 요청 금액: " + amount);
        }
        this.balance -= amount;
    }
}