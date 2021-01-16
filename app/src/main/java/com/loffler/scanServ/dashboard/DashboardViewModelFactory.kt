package com.loffler.scanServ.dashboard

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.loffler.scanServ.service.HardwareScanner
import com.loffler.scanServ.service.sql.dao.OutputDao
import com.loffler.scanServ.utils.AppLauncher
import com.loffler.scanServ.utils.QrCodeGenerator
import com.loffler.scanServ.utils.ResourceProvider

@Suppress("UNCHECKED_CAST")
class DashboardViewModelFactory(
    private val qrCodeScanner: HardwareScanner,
    private val appLauncher: AppLauncher,
    private val outputDao: OutputDao,
    private val preferences: SharedPreferences,
    private val qrCodeGenerator: QrCodeGenerator,
    private val resourceProvider: ResourceProvider
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return DashboardViewModel(qrCodeScanner, appLauncher, outputDao, preferences, qrCodeGenerator, resourceProvider) as T
    }
}