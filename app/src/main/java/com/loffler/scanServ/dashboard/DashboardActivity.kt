package com.loffler.scanServ.dashboard

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Base64
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.loffler.scanServ.*
import com.loffler.scanServ.cdcsetting.SharedPreferencesController
import com.loffler.scanServ.service.HardwareScannerImpl
import com.loffler.scanServ.service.ScannedContentValidator
import com.loffler.scanServ.service.sql.dao.OutputDaoImpl
import com.loffler.scanServ.service.sql.dao.ValidationDaoImpl
import com.loffler.scanServ.utils.*
import com.loffler.scanServ.welcomescreen.WelcomeActivity
import com.loffler.scanServ.welcomescreen.WelcomeDetectorActivity

class DashboardActivity : AppCompatActivity() {
    private lateinit var logoImg: ImageView
    private lateinit var instructionsTxt: TextView
    private lateinit var qrCodeImg: ImageView

    private val hardwareScanner: HardwareScannerImpl by lazy {
        val preferences = getSharedPreferences(Constants.PreferenceName, MODE_PRIVATE)
        val validationDao = ValidationDaoImpl(preferences)
        val validator = ScannedContentValidator(validationDao, preferences)
        HardwareScannerImpl(validator)
    }

    private val scanViewModel by lazy {
        val preferences = getSharedPreferences(Constants.PreferenceName, MODE_PRIVATE)
        val appLauncher = AppLauncherImpl(applicationContext)
        val outputDao = OutputDaoImpl(preferences)
        val resourceProvider = ResourceProviderImpl(applicationContext)
        val factory = DashboardViewModelFactory(hardwareScanner, appLauncher, outputDao, preferences, QrCodeGeneratorImpl(), resourceProvider)
        ViewModelProvider(this, factory)[DashboardViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        logoImg = findViewById(R.id.dashboard_img_logo)
        instructionsTxt = findViewById(R.id.dashboard_txt_verbiage)
        qrCodeImg = findViewById(R.id.dashboard_img_qr)

        findViewById<View>(R.id.dashboard_settings).setOnLongClickListener {
            finish()
            startActivity(Intent(baseContext, NavigationActivity::class.java))
            true
        }
        observe()
        startScanServService()
        if (SharedPreferencesController.with(applicationContext).getBoolean(Constants.WELCOME_ENABLE)) {
            val timeout = getSharedPreferences(Constants.PreferenceName, MODE_PRIVATE).getLong(Constants.DashboardSettingsReturnToForegroundTimeoutKey, 15L);
            object : CountDownTimer(timeout * 1000L, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    if (millisUntilFinished > 5000 && millisUntilFinished < 6000) {
                        val basic = DialogSpec.Basic(R.string.stay_too_long, getString(R.string.stay_security_too_long_message))
                        basic.closeTimeOut = 5000L
                        MessageProvider.showAlert(this@DashboardActivity, basic)
                    }
                }
                override fun onFinish() {
                    if (!applicationContext.isAppInBackground()) {
                        finish()
                    }
                }
            }.start()
        }
    }

    private fun observe() {
        scanViewModel.errorMessageEvent().observe(this, { errorMessage ->
            MessageProvider.showToast(this, errorMessage)
        })
        scanViewModel.toastMessage().observe(this, { message ->
            MessageProvider.showToast(this, message)
        })
        scanViewModel.dialogEvent().observe(this, { dialogSpec ->
            MessageProvider.showAlert(this, dialogSpec)
        })
        scanViewModel.scanId().observe(this, { dialogSpec ->
            SharedPreferencesController.with(applicationContext).saveInt(Constants.CURRENT_SCAN_ID, dialogSpec)
        })
        scanViewModel.showTimeoutDialog().observe(this, { isShow ->
            val timeout = SharedPreferencesController.with(applicationContext).getLong(Constants.DashboardSettingsReturnToForegroundTimeoutKey)
            AppLauncherImpl(applicationContext).launchMips()
            handlerTimeout = Handler()
            Utils.notificationArrived(this@DashboardActivity, getString(R.string.stay_too_long), getString(R.string.stay_mips_too_long_message), timeout, handlerTimeout)
//            if(SharedPreferencesController.with(applicationContext).getBoolean(Constants.WELCOME_ENABLE)){
//                finish()
//            }

        })
        scanViewModel.logo().observe(this, { logoUri ->
            if (logoUri == null) {
                val imageString = getSharedPreferences(Constants.PreferenceName, MODE_PRIVATE).getString(Constants.CompanyLogoKey, "");
                val decodedImage: ByteArray = Base64.decode(imageString, Base64.DEFAULT)
                logoImg.setImageBitmap(BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.size))
            } else {
                val imageString = getSharedPreferences(Constants.PreferenceName, MODE_PRIVATE).getString(Constants.DashboardSettingsLogoImage, "");
                val decodedImage: ByteArray = Base64.decode(imageString, Base64.DEFAULT)
                logoImg.setImageBitmap(BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.size))
            }
        })
        scanViewModel.qrCode().observe(this, { qrCodeBitmap ->
            qrCodeImg.setImageBitmap(qrCodeBitmap)
        })
        scanViewModel.instructions().observe(this, { text ->
            instructionsTxt.text = text
        })

    }
    companion object {
        private var handlerTimeout: Handler? = null

        fun stopHandler() {
            if (DashboardActivity.handlerTimeout != null) {
                DashboardActivity.handlerTimeout!!.removeCallbacksAndMessages(null)
                DashboardActivity.handlerTimeout = null
            }
        }
    }



    private fun startScanServService() {
        val servIntent = Intent(applicationContext, ScanService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(servIntent)
        } else {
            startService(servIntent)
        }
    }

    override fun onStart() {
        super.onStart()
        scanViewModel.loadSettings()
    }


    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        hardwareScanner.dispatchKeyEvent(event)
        return super.dispatchKeyEvent(event)
    }

    override fun finish() {
        super.finish()
        if (SharedPreferencesController.with(applicationContext).getBoolean(Constants.WELCOME_ENABLE)) {
            val dashboardIntent = Intent(baseContext, WelcomeDetectorActivity::class.java)
            startActivity(dashboardIntent)
        }
    }
}