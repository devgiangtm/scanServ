package com.loffler.scanServ.dashboard

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputLayout
import com.loffler.scanServ.BaseActivity
import com.loffler.scanServ.Constants
import com.loffler.scanServ.R
import com.loffler.scanServ.utils.*


class DashboardSettingsActivity : BaseActivity() {
    private lateinit var featureEnabledSwitch: SwitchMaterial
    private lateinit var validationEnabledSwitch: SwitchMaterial
    private lateinit var validationTableNameEdit: TextInputLayout
    private lateinit var verbiageInputLayout: TextInputLayout
    private lateinit var saveBtn: Button
    private lateinit var settingsLayout: ConstraintLayout
    private lateinit var focusTrickView: RelativeLayout
    private lateinit var validationNotifyMessageText: TextView
    private lateinit var descriptionLayout: LinearLayout
    private lateinit var validationLayout: ConstraintLayout
    private lateinit var validationErrorMessageEdit: TextInputLayout

    private val imagePicker = ImagePickerImpl(this)

    private val viewModel by lazy {
        val preferences = getSharedPreferences(Constants.PreferenceName, MODE_PRIVATE)
        val resourceProvider = ResourceProviderImpl(applicationContext)
        val factory = DashboardSettingsViewModelFactory(preferences, resourceProvider)
        ViewModelProvider(this, factory)[DashboardSettingsViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_settings)
        supportActionBar?.title = getString(R.string.title_dashboard_access)

        focusTrickView = findViewById(R.id.dashboard_settings_focus_trick)
        featureEnabledSwitch = findViewById(R.id.dashboard_settings_switch_enable_feature)
        validationEnabledSwitch = findViewById(R.id.dashboard_settings_switch_enable_validation)
        validationTableNameEdit = findViewById(R.id.dashboard_settings_edit_validation_table_name)
        verbiageInputLayout = findViewById(R.id.dashboard_settings_edit_instructions)
        saveBtn = findViewById(R.id.dashboard_settings_btn_save)
        settingsLayout = findViewById(R.id.dashboard_settings_layout)
        validationNotifyMessageText = findViewById(R.id.dashboard_settings_validation_txt_database_connection_error)
        descriptionLayout = findViewById(R.id.dashboard_settings_layout_feature_description)
        validationLayout = findViewById(R.id.dashboard_settings_validation_layout)
        validationErrorMessageEdit = findViewById(R.id.dashboard_settings_edit_validation_error_message)

        val decodedImage: ByteArray = Base64.decode(getSharedPreferences(Constants.PreferenceName, MODE_PRIVATE).getString(Constants.CompanyLogoKey, ""), Base64.DEFAULT)

        observe()
        setupListeners()
    }

    private fun observe() {
        viewModel.dashboardFeatureEnabled().observe(this, { isEnabled ->
            settingsLayout.visible(isEnabled)
            featureEnabledSwitch.isChecked = isEnabled
            descriptionLayout.visible(!isEnabled)
        })
        viewModel.instructions().observe(this, { text ->
            verbiageInputLayout.editText?.setText(text)
        })
        viewModel.validationEnabled().observe(this, { isEnabled ->
            validationTableNameEdit.visible(isEnabled)
            validationErrorMessageEdit.visible(isEnabled)
            validationEnabledSwitch.isChecked = isEnabled
        })
        viewModel.validationTableName().observe(this, { text ->
            validationTableNameEdit.editText?.setText(text)
        })
        viewModel.validationTableNameError().observe(this, { text ->
            validationTableNameEdit.error = text
        })
        viewModel.toastMessage().observe(this, { message ->
            MessageProvider.showToast(this, message)
        })
        viewModel.databaseConnected().observe(this, { isConnected ->
            validationNotifyMessageText.visible(!isConnected)
            featureEnabledSwitch.isEnabled = isConnected
        })
        viewModel.validationErrorMessage().observe(this, { text ->
            validationErrorMessageEdit.editText?.setText(text)
        })
        viewModel.loading().observe(this, { loading ->
            if (loading) {
                MessageProvider.showProgress(supportFragmentManager, DialogSpec.Loading(getString(R.string.message_dashboard_setting_loading)))
            } else {
                MessageProvider.hideProgress(supportFragmentManager)
            }
        })
    }

    private fun setupListeners() {
        saveBtn.setOnClickListener {
            confirmSave()
        }
        validationLayout.setOnClickListener {
            viewModel.enableValidation(!validationEnabledSwitch.isChecked)
        }
        featureEnabledSwitch.setOnCheckedChangeWhenPressedListener { isChecked -> viewModel.enableEntireFeature(isChecked) }
        validationEnabledSwitch.setOnCheckedChangeWhenPressedListener { isChecked -> viewModel.enableValidation(isChecked) }
        verbiageInputLayout.editText?.setOnLostFocusListener { text -> viewModel.updateInstructions(text) }
        validationTableNameEdit.editText?.setOnLostFocusListener { text -> viewModel.updateValidationTableName(text) }
        validationErrorMessageEdit.editText?.setOnLostFocusListener { text -> viewModel.updateValidationErrorMessage(text) }
    }

    private fun confirmSave() {
        focusTrickView.requestFocus()
        dismissKeyboard()
        viewModel.save()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        imagePicker.parseActivityResult(requestCode, resultCode, data)
    }

    private fun SwitchMaterial.setOnCheckedChangeWhenPressedListener(func: (isChecked: Boolean) -> Unit) {
        setOnCheckedChangeListener { view, isChecked ->
            if (view.isPressed) {
                func(isChecked)
            }
        }
    }

    private fun EditText.setOnLostFocusListener(func: (text: String) -> Unit) {
        setOnFocusChangeListener { view, hasFocus ->
            if (!hasFocus) {
                func(this.getTextOrEmpty())
            }
        }
    }


}