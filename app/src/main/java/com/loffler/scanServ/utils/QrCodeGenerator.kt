package com.loffler.scanServ.utils

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder


interface QrCodeGenerator {
    fun generate(content: String) : Bitmap?
}

class QrCodeGeneratorImpl : QrCodeGenerator {

    override fun generate(content: String) : Bitmap? {
        try {
            val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 200, 200)
            val barcodeEncoder = BarcodeEncoder()
            return barcodeEncoder.createBitmap(matrix)
        } catch (ex: WriterException) {
            ex.printStackTrace()
            return null
        } catch (ex: Exception) {
            return null
        }
    }
}