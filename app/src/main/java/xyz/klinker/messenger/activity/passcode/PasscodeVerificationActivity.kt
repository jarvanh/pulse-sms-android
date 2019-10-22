package xyz.klinker.messenger.activity.passcode

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Button
import android.widget.Toast
import xyz.klinker.android.floating_tutorial.FloatingTutorialActivity
import xyz.klinker.android.floating_tutorial.TutorialPage
import com.raycoarana.codeinputview.CodeInputView
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.Settings
import android.content.Intent
import android.net.Uri
import android.os.Looper
import android.widget.EditText
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import xyz.klinker.messenger.activity.main.MainColorController
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import java.util.concurrent.Executor

class PasscodeVerificationActivity : FloatingTutorialActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(Activity.RESULT_CANCELED)

        MainColorController(this).configureNavigationBarColor()
    }

    override fun getPages(): List<TutorialPage> {
        val hasAccount = Account.exists()

        return when {
            hasAccount -> listOf(PasscodePage(this), AccountPasswordPage(this))
            else -> listOf(PasscodePage(this))
        }
    }

    companion object {
        const val REQUEST_CODE = 185

        fun show(activity: FragmentActivity, onAuthenticated: () -> Unit) {
            val startPasscodePrompt: () -> Unit = {
                activity.startActivityForResult(Intent(activity, PasscodeVerificationActivity::class.java), REQUEST_CODE)
            }

            val biometricManager = BiometricManager.from(activity)
            if (biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle(activity.getString(R.string.biometric_prompt_title))
                        .setSubtitle(activity.getString(R.string.biometric_prompt_summary))
                        .setNegativeButtonText(activity.getString(R.string.passcode))
                        .setConfirmationRequired(false)
                        .build()

                val biometricPrompt = BiometricPrompt(activity, getMainThreadExecutor(),
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                onAuthenticated()
                            }

                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                super.onAuthenticationError(errorCode, errString)
                                if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                                    Toast.makeText(activity, errString, Toast.LENGTH_SHORT).show()
                                }

                                startPasscodePrompt()
                            }

                            override fun onAuthenticationFailed() {
                                super.onAuthenticationFailed()
                                Toast.makeText(activity, activity.getString(R.string.biometric_prompt_failed), Toast.LENGTH_SHORT).show()
                                startPasscodePrompt()
                            }
                        })

                biometricPrompt.authenticate(promptInfo)
            } else {
                startPasscodePrompt()
            }
        }

        private fun getMainThreadExecutor(): Executor {
            return MainThreadExecutor()
        }

        private class MainThreadExecutor : Executor {
            private val handler = Handler(Looper.getMainLooper())

            override fun execute(r: Runnable) {
                handler.post(r)
            }
        }
    }
}

@SuppressLint("ViewConstructor")
class PasscodePage(context: FloatingTutorialActivity) : TutorialPage(context) {

    private val nextButton: Button by lazy { findViewById<View>(R.id.tutorial_next_button) as Button }
    private val passcode: CodeInputView by lazy { findViewById<View>(R.id.code_input) as CodeInputView }

    override fun initPage() {
        setContentView(R.layout.page_passcode)
        setNextButtonText(R.string.forgot_passcode)

        val userPasscode = Settings.privateConversationsPasscode

        passcode.setShowKeyboard(true)
        passcode.inPasswordMode = true

        passcode.addOnCompleteListener { code ->
            if (code == userPasscode) {
                setActivityResult(Activity.RESULT_OK)
                getActivity().finishAnimated()
            } else {
                passcode.setError(R.string.incorrect_passcode)
                Handler().postDelayed({
                    passcode.setEditable(true)
                    passcode.clearError()
                    passcode.setShowKeyboard(true)
                    passcode.requestFocus()

                    try {
                        val method = passcode.javaClass.getDeclaredMethod("deleteCharacter")
                        method.isAccessible = true
                        method.invoke(passcode)
                        method.invoke(passcode)
                        method.invoke(passcode)
                        method.invoke(passcode)
                    } catch (e: Throwable) {
                    }
                }, 500)
            }
        }

        if (!Account.exists()) {
            nextButton.setOnClickListener {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://messenger.klinkerapps.com/forgot_passcode.html"))
                getActivity().startActivity(browserIntent)
            }
        }
    }

    override fun onShown(firstTimeShown: Boolean) {
        super.onShown(firstTimeShown)
        passcode.requestFocus()
    }
}

@SuppressLint("ViewConstructor")
class AccountPasswordPage(context: FloatingTutorialActivity) : TutorialPage(context) {

    private val nextButton: Button by lazy { findViewById<View>(R.id.tutorial_next_button) as Button }

    private val email: EditText by lazy { findViewById<View>(R.id.login_email) as EditText }
    private val password: EditText by lazy { findViewById<View>(R.id.login_password) as EditText }

    override fun initPage() {
        setContentView(R.layout.page_password_verification)
        setNextButtonText(R.string.verify)

        nextButton.setOnClickListener {
            verifyAccount(email.text.toString(), password.text.toString())
        }
    }

    override fun onShown(firstTimeShown: Boolean) {
        super.onShown(firstTimeShown)
        email.requestFocus()
    }

    private fun verifyAccount(email: String, password: String) {
        if (password.isBlank()) {
            Toast.makeText(getActivity(), R.string.api_no_password, Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = ProgressDialog(getActivity())
        dialog.setMessage(getActivity().getString(xyz.klinker.messenger.api.implementation.R.string.api_connecting))
        dialog.show()

        Thread {
            val response = ApiUtils.login(email, password)
            getActivity().runOnUiThread {
                try {
                    dialog.dismiss()
                } catch (e: Exception) {
                }

                if (response == null) {
                    Toast.makeText(getActivity(), xyz.klinker.messenger.api.implementation.R.string.api_login_error,
                            Toast.LENGTH_SHORT).show()
                } else {
                    setActivityResult(Activity.RESULT_OK)
                    getActivity().finishAnimated()
                }
            }
        }.start()
    }
}