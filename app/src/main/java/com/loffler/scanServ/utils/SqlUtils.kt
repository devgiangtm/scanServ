package com.loffler.scanServ.utils

import com.loffler.scanServ.service.sql.ErrorCode
import com.loffler.scanServ.service.sql.dao.DaoResult
import java.lang.Exception
import java.sql.SQLException
import java.sql.SQLTimeoutException

fun SQLException.mapErrorCode(): ErrorCode {
    return when (errorCode) {
        207 -> ErrorCode.InvalidColumnName
        208 -> ErrorCode.InvalidTableName
        else -> ErrorCode.Unknown
    }
}

fun Exception.handleSqlError(): DaoResult.Error {
    return when (this) {
        is SQLException -> DaoResult.Error(this.mapErrorCode())
        is SQLTimeoutException -> DaoResult.Error(ErrorCode.TimeoutException)
        else -> DaoResult.Error(ErrorCode.Unknown)
    }
}