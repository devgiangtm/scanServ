package com.loffler.scanServ.service.sql.dao

import android.content.SharedPreferences
import com.loffler.scanServ.Constants
import com.loffler.scanServ.SQLHelper
import com.loffler.scanServ.service.sql.ErrorCode
import com.loffler.scanServ.utils.handleSqlError
import java.lang.Exception

interface ValidationDao {
    fun getCodeByName(qrCode: String): DaoResult<String>
    fun getIdCardByNumber(idCardNumber: String): DaoResult<String>
}

class ValidationDaoImpl(
    private val preferences: SharedPreferences
) : ValidationDao {

    companion object {
        private const val COL_ID_NUMBER = "idCardNumber"
    }

    override fun getCodeByName(qrCode: String): DaoResult<String> {
        val connection = SQLHelper.getConnection(preferences) ?: return DaoResult.Error(ErrorCode.DatabaseConnection)

        try {
            connection.use {
                val tableName = preferences.getString(Constants.DashboardSettingsValidationTableNameKey, "")
                val statement = connection.prepareStatement("SELECT * FROM $tableName WHERE $COL_ID_NUMBER=?")

                statement.use {
                    val result = statement.apply { it.setString(1, qrCode) }.executeQuery()

                    result.use {
                        return if (result.next()) {
                            DaoResult.Success(result.getString(COL_ID_NUMBER))
                        } else {
                            DaoResult.Success("")
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            return ex.handleSqlError()
        }
    }

    override fun getIdCardByNumber(idCardNumber: String): DaoResult<String> {
        TODO("Not yet implemented")
    }
}