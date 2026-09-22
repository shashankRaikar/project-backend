-- ==========================================================
-- Database and Schema Setup for Registration and Login App
-- Database: registration_login_db
-- ==========================================================

CREATE DATABASE IF NOT EXISTS `registration_login_db`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `registration_login_db`;

-- ----------------------------------------------------------
-- Table 1: `user`
-- Stores registered user accounts with BCrypt-hashed passwords
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `email` VARCHAR(100) NOT NULL,
    `phone_number` VARCHAR(20) NOT NULL,
    INDEX `idx_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------
-- Table 2: `jwt_token`
-- Stores generated JWTs, associated user reference, and timestamps
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `jwt_token` (
    `token_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `token` VARCHAR(1000) NOT NULL,
    `creation_time` DATETIME NOT NULL,
    `expiry_time` DATETIME NOT NULL,
    INDEX `idx_jwt_token_user` (`user_id`),
    INDEX `idx_jwt_token_value` (`token`(255)),
    CONSTRAINT `fk_jwt_user` FOREIGN KEY (`user_id`)
        REFERENCES `user` (`id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
