package com.loffler.scanServ.utils

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import com.loffler.scanServ.ScanServerApp

fun View.visible(visible: Boolean) {
    this.visibility = if (visible) View.VISIBLE else View.GONE
}

fun EditText.getTextOrEmpty() = this.text?.toString().orEmpty()

fun Context.dismissKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun AppCompatActivity.dismissKeyboard() {
    baseContext.dismissKeyboard(window.decorView)
}

fun ImageView.setImageBitmapOrPlaceholder(bitmap: Bitmap?, @DrawableRes placeholderRes: Int) {
    if (bitmap != null) {
        setImageBitmap(bitmap)
    } else {
        setImageResource(placeholderRes)
    }
}

fun ImageView.setImageUriOrPlaceholder(uri: Uri?, @DrawableRes placeholderRes: Int) {
    if (uri != null) {
        setImageURI(uri)
    } else {
        setImageResource(placeholderRes)
    }
}

fun StringBuffer.clear() {
    delete(0, length)
}

fun Context.isAppInBackground(): Boolean {
    val applicationContext = this.applicationContext
    return applicationContext is ScanServerApp && applicationContext.isAppInBackground
}