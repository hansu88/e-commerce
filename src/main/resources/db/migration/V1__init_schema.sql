-- ========================================
-- E-commerce Database Schema Migration
-- Version: 1.0
-- Description: Initial schema with optimized indexes (7 indexes)
-- ========================================

-- Drop existing indexes (if any)
DROP INDEX IF EXISTS idx_status_created ON products;
DROP INDEX IF EXISTS idx_name ON products;
DROP INDEX IF EXISTS idx_product_stock ON product_options;
DROP INDEX IF EXISTS idx_status_created ON orders;
DROP INDEX IF EXISTS idx_option_created ON order_items;
DROP INDEX IF EXISTS idx_option_created ON stock_histories;

-- Drop existing tables (if any)
DROP TABLE IF EXISTS stock_histories;
DROP TABLE IF EXISTS user_coupons;
DROP TABLE IF EXISTS coupons;
DROP TABLE IF EXISTS cart_items;
DROP TABLE IF EXISTS carts;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS product_options;
DROP TABLE IF EXISTS products;

-- ========================================
-- 1. Products Table
-- ========================================
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- 2. Product Options Table
-- ========================================
CREATE TABLE product_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    color VARCHAR(50) NOT NULL,
    size VARCHAR(50) NOT NULL,
    stock INT NOT NULL,
    version BIGINT DEFAULT 0,
    created_at DATETIME(6),
    updated_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index 1: ProductOption 조회용
CREATE INDEX idx_product_id ON product_options(product_id);

-- ========================================
-- 3. Orders Table
-- ========================================
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_amount INT NOT NULL,
    discount_amount INT NOT NULL DEFAULT 0,
    user_coupon_id BIGINT,
    created_at DATETIME(6),
    updated_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index 2: User의 주문 이력 조회용
CREATE INDEX idx_user_created ON orders(user_id, created_at);

-- ========================================
-- 4. Order Items Table
-- ========================================
CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_option_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    price INT NOT NULL,
    created_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index 3: Order의 OrderItem 조회용
CREATE INDEX idx_order_id ON order_items(order_id);

-- ========================================
-- 5. Carts Table
-- ========================================
CREATE TABLE carts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index 4: User의 Cart 조회용 (unique)
CREATE UNIQUE INDEX idx_user_id ON carts(user_id);

-- ========================================
-- 6. Cart Items Table
-- ========================================
CREATE TABLE cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_option_id BIGINT NOT NULL,
    quantity INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index 5: Cart의 CartItem 조회용
CREATE INDEX idx_cart_id ON cart_items(cart_id);

-- ========================================
-- 7. Coupons Table
-- ========================================
CREATE TABLE coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(100) NOT NULL,
    discount_amount INT NOT NULL,
    total_quantity INT NOT NULL,
    issued_quantity INT NOT NULL DEFAULT 0,
    valid_until DATETIME(6) NOT NULL,
    version BIGINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index 6: Coupon 코드 조회용 (unique)
CREATE UNIQUE INDEX idx_code ON coupons(code);

-- ========================================
-- 8. User Coupons Table
-- ========================================
CREATE TABLE user_coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    issued_at DATETIME(6) NOT NULL,
    used_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index 7: User의 쿠폰 조회용
CREATE INDEX idx_user_status ON user_coupons(user_id, status);

-- ========================================
-- 9. Stock Histories Table
-- ========================================
CREATE TABLE stock_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_option_id BIGINT NOT NULL,
    change_quantity INT NOT NULL,
    reason VARCHAR(100) NOT NULL,
    created_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- No index for stock_histories (history 조회는 빈도가 낮음)

-- ========================================
-- Index Summary (Total: 7 indexes)
-- ========================================
-- 1. product_options.idx_product_id
-- 2. orders.idx_user_created
-- 3. order_items.idx_order_id
-- 4. carts.idx_user_id (UNIQUE)
-- 5. cart_items.idx_cart_id
-- 6. coupons.idx_code (UNIQUE)
-- 7. user_coupons.idx_user_status
-- ========================================
