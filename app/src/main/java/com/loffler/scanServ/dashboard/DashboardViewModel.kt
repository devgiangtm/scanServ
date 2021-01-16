package com.loffler.scanServ.dashboard

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loffler.scanServ.Constants
import com.loffler.scanServ.R
import com.loffler.scanServ.service.HardwareScanner
import com.loffler.scanServ.service.ScanValidator.*
import com.loffler.scanServ.service.sql.dao.DaoResult
import com.loffler.scanServ.service.sql.dao.OutputDao
import com.loffler.scanServ.utils.AppLauncher
import com.loffler.scanServ.utils.DialogSpec
import com.loffler.scanServ.utils.QrCodeGenerator
import com.loffler.scanServ.utils.ResourceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardViewModel(
        private val qrCodeScanner: HardwareScanner,
        private val appLauncher: AppLauncher,
        private val outputsDao: OutputDao,
        private val preferences: SharedPreferences,
        private val qrCodeGenerator: QrCodeGenerator,
        private val resourceProvider: ResourceProvider
) : ViewModel(), HardwareScanner.Listener {
    private val logo = MutableLiveData<Uri?>()
    private val inAppGeneratedQrCode = MutableLiveData<Bitmap?>()
    private val instructions = MutableLiveData<String>()
    private val errorMessage = MutableLiveData<Int>()
    private val dialogMessage = MutableLiveData<DialogSpec>()
    private val toast = MutableLiveData<String>()

    fun errorMessageEvent(): LiveData<Int> = errorMessage
    fun dialogEvent(): LiveData<DialogSpec> = dialogMessage
    fun toastMessage(): LiveData<String> = toast
    fun logo(): LiveData<Uri?> = logo
    fun qrCode(): LiveData<Bitmap?> = inAppGeneratedQrCode
    fun instructions(): LiveData<String> = instructions

    init {
        qrCodeScanner.subscribe(this)
    }

    fun loadSettings() {
        with(preferences) {
            logo.value = getString(Constants.DashboardSettingsLogoImagePathKey, null)?.let { path -> Uri.parse(path) }
            inAppGeneratedQrCode.value = getString(Constants.DashboardSettingsQrCodeContentKey, null)?.let { qrCodeContent -> qrCodeGenerator.generate(qrCodeContent) }
            instructions.value = getString(Constants.DashboardSettingsInstructionsTextKey, null)
        }
    }

    fun Context.toast(message: CharSequence) =
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    override fun onRecognized(content: String) {
        viewModelScope.launch {
            when (qrCodeScanner.isCodeValid(content)) {
                State.Valid -> proceedTemperatureScan(content)
                State.Invalid -> {
                    val message = preferences.getString(Constants.DashboardSettingsValidationFailedMessageKey, null).orEmpty()
                    dialogMessage.value = DialogSpec.Basic(R.string.error_validation_qr_code, message)
                }
                State.Unreachable -> dialogMessage.value = DialogSpec.Basic(R.string.error, resourceProvider.getString(R.string.error_message_generic))
            }
        }
    }

    private suspend fun proceedTemperatureScan(content: String) {
        val replaceStr = content.replace(0.toChar().toString(), "")
        var pms = false;
        var welcomeEnable = false;
        with(preferences) {
            pms = getBoolean(Constants.swSQLWriteLogs, false)
            welcomeEnable = getBoolean(Constants.WELCOME_ENABLE, false)
        }
        if (pms) {
            val insertResult = withContext(Dispatchers.IO) { outputsDao.insert(OutputDao.Record.Insert(replaceStr)) }
            when (insertResult) {
                is DaoResult.Success -> {
                    appLauncher.launchEzPass()
                }
                is DaoResult.Error -> toast.value = "Unable to proceed. Please contact your administrator"
            }
        } else {
            appLauncher.launchEzPass()
        }

    }


    override fun onCleared() {
        super.onCleared()
        qrCodeScanner.unsubscribe()
    }
}