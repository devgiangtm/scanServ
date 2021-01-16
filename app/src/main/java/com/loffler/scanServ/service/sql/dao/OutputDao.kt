package com.loffler.scanServ.service.sql.dao

import android.content.SharedPreferences
import com.loffler.scanServ.Constants
import com.loffler.scanServ.SQLHelper
import com.loffler.scanServ.cdcsetting.SharedPreferencesController
import com.loffler.scanServ.service.sql.ErrorCode
import com.loffler.scanServ.utils.handleSqlError
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.*
import kotlin.coroutines.coroutineContext


interface OutputDao {
    sealed class Record(
            val macAddress: String?,
            val employeeId: String?,
            val scanTime: Date?,
            val temperature: Double?,
            val userId: String?,
            val type: String?,
            val mask: String?,
            val scannedId: String,
            val pending: Boolean,
            val created: Date,
            var question1: String?,
            var question2: String?,
            var question3: String?,
            var question4: String?,
            var result: String?

    ) {
        class Insert(scannedId: String) : Record(null, null, null, null, null, null, null, scannedId, true, Date(), null, null, null, null, null)
        class Update(
                macAddress: String,
                employeeId: String,
                scanTime: Date,
                temperature: Double,
                userId: String,
                type: String,
                mask: String,
                question1: String?,
                question2: String?,
                question3: String?,
                question4: String?,
                result: String?
        ) : Record(macAddress, employeeId, scanTime, temperature, userId, type, mask, "", false, Date(), question1, question2, question3, question4, result)
    }

    fun insert(record: Record.Insert): Int
    fun update(record: Record.Update, scanId: Int): DaoResult<Boolean>
    fun createTable(tableName: String): Boolean
}

class OutputDaoImpl(
        private val preferences: SharedPreferences
) : OutputDao {
    companion object {
        private const val COLUMN_MAC_ADDRESS = "macAddress"
        private const val COLUMN_EMPLOYEE_ID = "employeeID"
        private const val COLUMN_SCAN_TIME = "scanTime"
        private const val COLUMN_TEMPERATURE = "temperature"
        private const val COLUMN_USER_ID = "userId"
        private const val COLUMN_TYPE = "type"
        private const val COLUMN_MASK = "mask"
        private const val COLUMN_SCANNED_ID = "scannedId"
        private const val COLUMN_PENDING = "pending"
        private const val COLUMN_CREATED = "created"
        private const val COLUMN_QUESTION1 = "question1"
        private const val COLUMN_QUESTION2 = "question2"
        private const val COLUMN_QUESTION3 = "question3"
        private const val COLUMN_QUESTION4 = "question4"
        private const val COLUMN_RESULT = "result"
        private const val CODE_COMPLETED_SUCCESSFULLY = 1
    }

    override fun update(record: OutputDao.Record.Update, scanId: Int): DaoResult<Boolean> {
        val connection = SQLHelper.getConnection(preferences)
                ?: return DaoResult.Error(ErrorCode.DatabaseConnection)

        try {
            connection.use {
                val statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)

                statement.use {
                    val tableName = preferences.getString(Constants.SQLTableName, "")
                    val query = "SELECT  * FROM $tableName WHERE $COLUMN_PENDING=1 AND id = $scanId"
                    val updateResult = it.executeQuery(query)
                    return if (updateResult.next()) {
                        updateResult.apply {
                            first()
                            updateString(COLUMN_MAC_ADDRESS, record.macAddress)
                            updateString(COLUMN_EMPLOYEE_ID, record.employeeId)
                            updateTimestamp(COLUMN_SCAN_TIME, record.scanTime?.let { date -> Timestamp(date.time) })
                            updateDouble(COLUMN_TEMPERATURE, record.temperature ?: 0.0)
                            updateString(COLUMN_USER_ID, record.userId)
                            updateString(COLUMN_TYPE, record.type)
                            updateString(COLUMN_MASK, record.mask)
                            updateBoolean(COLUMN_PENDING, record.pending)
                            updateString(COLUMN_QUESTION1, record.question1)
                            updateString(COLUMN_QUESTION2, record.question2)
                            updateString(COLUMN_QUESTION3, record.question3)
                            updateString(COLUMN_QUESTION4, record.question4)
                            updateString(COLUMN_RESULT, record.result)
                            updateRow()
                        }
                        DaoResult.Success(true)
                    } else {
                        DaoResult.Success(false)
                    }
                }
            }
        } catch (ex: Exception) {
            return ex.handleSqlError()
        }
    }

    override fun insert(record: OutputDao.Record.Insert): Int {
        val connection = SQLHelper.getConnection(preferences)
                ?: return -1

        try {
            connection.use {
                val tableName = preferences.getString(Constants.SQLTableName, "")
                val statement = connection.prepareStatement("INSERT INTO $tableName VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", arrayOf("id")).apply {
                    setString(1, record.macAddress)
                    setString(2, record.employeeId)
                    setTimestamp(3, record.scanTime?.let { date -> Timestamp(date.time) })
                    setDouble(4, record.temperature ?: 0.0)
                    setString(5, record.userId)
                    setString(6, record.type)
                    setString(7, record.mask)
                    setString(8, record.scannedId)
                    setBoolean(9, true)
                    setTimestamp(10, Timestamp(record.created.time))
                    setString(11, record.question1)
                    setString(12, record.question2)
                    setString(13, record.question3)
                    setString(14, record.question4)
                    setString(15, record.result)
                }

                statement.use {
                    val updateResult = statement.executeUpdate()
                    val rs = statement.generatedKeys
                    if (rs.next()) {
                        val id = rs.getInt("id")
                        return id
                    } else {
                        return -1
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            return -1
        }
    }

    override fun createTable(tableName: String): Boolean {
//        val connection = SQLHelper.getConnection(preferences)
//                ?: return DaoResult.Error(ErrorCode.DatabaseConnection)
        val connection = SQLHelper.getConnection(preferences)
                ?: return false

        try {
            connection.use {
                val statement = connection.prepareStatement("create table " + tableName + " (id int identity constraint " + tableName + "_pk primary key nonclustered, macAddress text, employeeID text, scanTime datetime, temperature float, userId text, type text, mask text, scannedId text, pending tinyint, created datetime, question1 text, question2 text, question3 text, question4 text, result text )").apply {
                }

                statement.use {
                    val updateResult = statement.executeUpdate()
//                    return DaoResult.Success(updateResult == CODE_COMPLETED_SUCCESSFULLY)
                    return updateResult == 0
                }
            }
        } catch (ex: Exception) {
//            return ex.handleSqlError()
            return false
        }
    }



    //result.getString("scannedId").replace(0.toChar().toString(),"")
    fun trimZeros(str: String): String? {
        val pos = str.indexOf(0.toChar())
        return if (pos == -1) str else str.substring(0, pos)
    }
}
