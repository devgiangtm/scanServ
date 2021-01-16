package com.loffler.scanServ.utils

import android.content.Context
import androidx.annotation.StringRes

interface ResourceProvider {
    fun getString(@StringRes resId: Int): String
}

class ResourceProviderImpl(val context: Context) : ResourceProvider {
    override fun getString(resId: Int): String = context.getString(resId)
}