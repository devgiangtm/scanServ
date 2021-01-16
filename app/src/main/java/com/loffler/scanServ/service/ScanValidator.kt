package com.loffler.scanServ.service

import android.content.SharedPreferences
import com.loffler.scanServ.Constants
import com.loffler.scanServ.service.ScanValidator.*
import com.loffler.scanServ.service.sql.dao.ValidationDao
import com.loffler.scanServ.service.sql.dao.DaoResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ScanValidator {
    enum class State {
        Valid,
        Invalid,
        Unreachable
    }
    suspend fun validate(code: String) : State
}

class ScannedContentValidator(
    private val dao: ValidationDao,
    private val preferences: SharedPreferences
) : ScanValidator {

    override suspend fun validate(code: String): State {
        val isValidationEnabled = preferences.getBoolean(Constants.DashboardSettingsValidationTableEnableFeatureKey, false)

        return if (isValidationEnabled) {
            getValidationState(code)
        } else {
            State.Valid
        }
    }

    private suspend fun getValidationState(code: String): State {
        return withContext(Dispatchers.IO) {
            when (val result = dao.getCodeByName(code)) {
                is DaoResult.Success -> if (result.content.isNotEmpty()) State.Valid else State.Invalid
                is DaoResult.Error -> State.Unreachable
            }
        }
    }
}