package com.loffler.scanServ.dashboard

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.loffler.scanServ.utils.ImagePicker
import com.loffler.scanServ.utils.ResourceProvider

class DashboardSettingsViewModelFactory(
    private val preferences: SharedPreferences,
    private val resourceProvider: ResourceProvider
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return DashboardSettingsViewModel(preferences, resourceProvider) as T
    }
}