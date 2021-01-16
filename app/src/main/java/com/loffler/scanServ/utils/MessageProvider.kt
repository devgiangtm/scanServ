package com.loffler.scanServ.utils

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.loffler.scanServ.R
import java.util.concurrent.TimeUnit

sealed class DialogSpec(@StringRes val title: Int, val message: String, val closeTimeOut: Long? = TimeUnit.SECONDS.toMillis(3)) {
    class PositiveAction(@StringRes val buttonName: Int, val action: (() -> Unit)?) {
        companion object {
            fun default() = PositiveAction(R.string.ok, null)
        }
    }

    class Basic(title: Int, message: String) : DialogSpec(title, message)
    class Alert(title: Int, message: String, val positiveAction: PositiveAction = PositiveAction.default()) : DialogSpec(title, message)
    class Loading(message: String) : DialogSpec(-1, message)
}

object MessageProvider {
    private const val TAG_PROGRESS = "progress_tag"

    @JvmStatic
    fun showAlert(context: Context, dialogSpec: DialogSpec) {
        val builder = build(context, dialogSpec)
        val dialog = builder.show()

        dialogSpec.closeTimeOut?.let { timeout ->
            Handler(Looper.getMainLooper()).postDelayed({ dialog.dismiss() }, timeout)
        }
    }

    private fun build(context: Context, dialogSpec: DialogSpec): AlertDialog.Builder {
        return AlertDialog.Builder(context).apply {

            when (dialogSpec) {
                is DialogSpec.Basic -> {
                    setTitle(dialogSpec.title)
                    setMessage(dialogSpec.message)
                }
                is DialogSpec.Alert -> {
                    setTitle(dialogSpec.title)
                    setMessage(dialogSpec.message)
                    setPositiveButton(dialogSpec.positiveAction.buttonName) { _, _ -> dialogSpec.positiveAction.action?.invoke() }
                }
            }
        }
    }

    @JvmStatic
    fun showProgress(fragmentManager: FragmentManager, dialogSpec: DialogSpec.Loading) {
        val progressDialog = ScanServProgressDialog.newInstance(dialogSpec.message)
        progressDialog.show(fragmentManager, TAG_PROGRESS)
    }

    @JvmStatic
    fun hideProgress(fragmentManager: FragmentManager) {
        val progressDialog = fragmentManager.findFragmentByTag(TAG_PROGRESS)

        if (progressDialog != null) {
            fragmentManager.beginTransaction()
                .remove(progressDialog)
                .commit()
        }
    }

    @JvmStatic
    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    @JvmStatic
    fun showToast(context: Context, @StringRes message: Int) {
        showToast(context, context.getString(message))
    }
}

class ScanServProgressDialog : DialogFragment() {
    companion object {
        private const val KEY_MESSAGE = "message_key"

        fun newInstance(message: String) = ScanServProgressDialog().apply {
            arguments = Bundle().apply { putString(KEY_MESSAGE, message) }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = View.inflate(context, R.layout.dialog_progress, null)
        val arguments = requireArguments()
        val message = arguments.getString(KEY_MESSAGE)

        view.findViewById<TextView>(R.id.progress_message).text = message

        return AlertDialog.Builder(requireActivity())
            .setView(view)
            .create()
    }
}