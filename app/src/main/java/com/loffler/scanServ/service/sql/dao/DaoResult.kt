package com.loffler.scanServ.service.sql.dao

import com.loffler.scanServ.service.sql.ErrorCode

sealed class DaoResult<out T> {
    class Success<out T>(val content: T) : DaoResult<T>()
    class Error(val errorCode: ErrorCode) : DaoResult<Nothing>()
}