package com.loffler.scanServ.utils

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CompletableDeferred


interface ImagePicker {
    suspend fun pickImage(): Uri?
}

class ImagePickerImpl(
    private val context: AppCompatActivity
) : ImagePicker {
    companion object {
        const val PICK_IMAGE_CODE = 100
        const val TYPE_IMAGE = "image/*"
    }

    private lateinit var completable: CompletableDeferred<Uri?>

    override suspend fun pickImage(): Uri? {
        completable = CompletableDeferred()

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = TYPE_IMAGE
            flags = flags or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        val chooser = Intent.createChooser(intent, "Select Picture")
        context.startActivityForResult(chooser, PICK_IMAGE_CODE)
        return completable.await()
    }

    fun parseActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE_CODE) {
            val imageUri = data?.data?.also { uri ->
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            completable.complete(imageUri)
        } else {
            completable.complete(null)
        }
    }
}
