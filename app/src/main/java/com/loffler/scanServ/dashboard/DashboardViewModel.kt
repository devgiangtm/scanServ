package com.loffler.scanServ.dashboard

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
import com.loffler.scanServ.Utils
import com.loffler.scanServ.service.HardwareScanner
import com.loffler.scanServ.service.ScanValidator.*
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
    private val logo = MutableLiveData<String>()
    private val inAppGeneratedQrCode = MutableLiveData<Bitmap?>()
    private val instructions = MutableLiveData<String>()
    private val errorMessage = MutableLiveData<Int>()
    private val dialogMessage = MutableLiveData<DialogSpec>()
    private val toast = MutableLiveData<String>()
    private val scanId = MutableLiveData<Int>()
    private val showTimeoutDialog = MutableLiveData<Boolean>()

    fun errorMessageEvent(): LiveData<Int> = errorMessage
    fun dialogEvent(): LiveData<DialogSpec> = dialogMessage
    fun scanId(): LiveData<Int> = scanId
    fun showTimeoutDialog(): LiveData<Boolean> = showTimeoutDialog
    fun toastMessage(): LiveData<String> = toast
    fun logo(): LiveData<String> = logo
    fun qrCode(): LiveData<Bitmap?> = inAppGeneratedQrCode
    fun instructions(): LiveData<String> = instructions

    init {
        qrCodeScanner.subscribe(this)
    }

    fun loadSettings() {
        with(preferences) {
            logo.value = getString(Constants.DashboardSettingsLogoImage, null)
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
            if (insertResult == -1) {
                toast.value = "Unable to proceed. Please contact your administrator"
            } else {
                scanId.value = insertResult
//                appLauncher.launchEzPass()
                showTimeoutDialog.value = true
            }
        } else {
//            appLauncher.launchEzPass()
            showTimeoutDialog.value = true
        }

    }

    override fun onCleared() {
        super.onCleared()
        qrCodeScanner.unsubscribe()
    }
}