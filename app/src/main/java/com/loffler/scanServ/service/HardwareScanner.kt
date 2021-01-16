package com.loffler.scanServ.service

import android.view.KeyEvent
import com.loffler.scanServ.utils.clear

interface HardwareScanner {
    interface Listener {
        fun onRecognized(content: String)
    }

    fun subscribe(subscriber: Listener)
    fun unsubscribe()

    suspend fun isCodeValid(code: String): ScanValidator.State
}

class HardwareScannerImpl(
    private val validator: ScanValidator
) : HardwareScanner {
    private var subscriber: HardwareScanner.Listener? = null
    private val qrCodeContent = StringBuffer()

    override fun subscribe(subscriber: HardwareScanner.Listener) {
        this.subscriber = subscriber
    }

    override fun unsubscribe() {
        this.subscriber = null
    }

    override suspend fun isCodeValid(code: String): ScanValidator.State {
        return validator.validate(code)
    }

    fun dispatchKeyEvent(event: KeyEvent?) {
        if (event?.action == KeyEvent.ACTION_DOWN) {
            val pressedKey = event.unicodeChar.toChar()

            if (pressedKey.toString() != System.lineSeparator()) {
                qrCodeContent.append(pressedKey)
            }

            if (qrCodeContent.isNotEmpty() && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                val decodedQrCodeContent = qrCodeContent.toString()
                subscriber?.onRecognized(decodedQrCodeContent)
                qrCodeContent.clear()
            }
        }
    }
}