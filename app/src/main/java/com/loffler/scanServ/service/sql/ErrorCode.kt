package com.loffler.scanServ.service.sql

enum class ErrorCode {
    InvalidTableName,
    InvalidColumnName,
    TimeoutException,
    DatabaseConnection,
    Unknown
}