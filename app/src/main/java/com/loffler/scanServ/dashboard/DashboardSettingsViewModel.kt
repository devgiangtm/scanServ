package com.loffler.scanServ.dashboard

import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loffler.scanServ.Constants
import com.loffler.scanServ.R
import com.loffler.scanServ.SQLHelper
import com.loffler.scanServ.utils.ImagePicker
import com.loffler.scanServ.utils.ResourceProvider
import com.loffler.scanServ.utils.write
import kotlinx.coroutines.*

class DashboardSettingsViewModel(
    private val preferences: SharedPreferences,
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val dashboardFeatureEnabled = MutableLiveData<Boolean>()
    private val instructions = MutableLiveData<String>()
    private val validationEnabled = MutableLiveData<Boolean>()
    private val validationTableName = MutableLiveData<String>()
    private val validationTableNameError = MutableLiveData<String?>()
    private val toastMessage = MutableLiveData<String>()
    private val databaseConnected = MutableLiveData<Boolean>()
    private val loading = MutableLiveData<Boolean>()
    private val validationErrorMessage = MutableLiveData<String>()

    fun dashboardFeatureEnabled(): LiveData<Boolean> = dashboardFeatureEnabled
    fun instructions(): LiveData<String> = instructions
    fun validationEnabled(): LiveData<Boolean> = validationEnabled
    fun validationTableName(): LiveData<String> = validationTableName
    fun validationTableNameError(): LiveData<String?> = validationTableNameError
    fun toastMessage(): LiveData<String> = toastMessage
    fun databaseConnected(): LiveData<Boolean> = databaseConnected
    fun loading(): LiveData<Boolean> = loading
    fun validationErrorMessage(): LiveData<String> = validationErrorMessage

    init {
        loadSettings()
        checkDatabaseConnection()
    }

    private fun loadSettings() {
        with(preferences) {
            getBoolean(Constants.DashboardSettingsEnableFeatureKey, false).let { isEnabled ->
                enableEntireFeature(isEnabled)
            }
            instructions.value = getString(Constants.DashboardSettingsInstructionsTextKey, null)
            getBoolean(Constants.DashboardSettingsValidationTableEnableFeatureKey, false).let { isEnabled ->
                enableValidation(isEnabled)
            }
            validationTableName.value = getString(Constants.DashboardSettingsValidationTableNameKey, null)
            validationErrorMessage.value = getString(Constants.DashboardSettingsValidationFailedMessageKey, resourceProvider.getString(R.string.message_validation_failed_default))
        }
    }

    private fun checkDatabaseConnection() {
        viewModelScope.launch {
            loading.value = true
            val connected = isDatabaseConnected()

            if (!connected) {
                enableEntireFeature(false)
            }

            databaseConnected.value = connected
            loading.value = false
        }
    }

    private suspend fun isDatabaseConnected(): Boolean {
        return withContext(Dispatchers.IO) {
            SQLHelper.isDatabaseConnected(preferences)
        }
    }



    fun enableEntireFeature(enable: Boolean) {
        dashboardFeatureEnabled.value = enable
        preferences.write(Constants.DashboardSettingsEnableFeatureKey, enable)
    }

    fun updateInstructions(text: String) {
        instructions.value = text
    }

    fun enableValidation(enable: Boolean) {
        validationEnabled.value = enable
    }

    fun updateValidationTableName(tableName: String) {
        resetErrors()
        validationTableName.value = tableName
    }


    fun updateValidationErrorMessage(text: String) {
        validationErrorMessage.value = text
    }

    fun save() {
        if (validate()) {
            val editor = preferences.edit()

            with(editor) {
                putString(Constants.DashboardSettingsInstructionsTextKey, instructions.value)
                putBoolean(Constants.DashboardSettingsValidationTableEnableFeatureKey, validationEnabled.value ?: false)
                putString(Constants.DashboardSettingsValidationTableNameKey, validationTableName.value)
                putString(Constants.DashboardSettingsValidationFailedMessageKey, validationErrorMessage.value)
            }
            editor.apply()

            toastMessage.value = "Changes Saved"
        }
    }

    private fun validate(): Boolean {
        resetErrors()

        if (validationEnabled.value == true && validationTableName.value.isNullOrEmpty()) {
            validationTableNameError.value = "Table name should be set."
            return false
        }

        return true
    }

    private fun resetErrors() {
        validationTableNameError.value = null
    }


    val isFeatureEnabled: Boolean get() = dashboardFeatureEnabled.value == true
}