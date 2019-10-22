package xyz.klinker.messenger.activity.passcode

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import xyz.klinker.android.floating_tutorial.FloatingTutorialActivity
import xyz.klinker.android.floating_tutorial.TutorialPage
import com.raycoarana.codeinputview.CodeInputView
import xyz.klinker.messenger.activity.main.MainColorController
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.Settings

class PasscodeSetupActivity : FloatingTutorialActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        MainColorController(this).configureNavigationBarColor()
    }

    override fun getPages(): List<TutorialPage> = listOf(PasscodeSetupPage(this))
}

@SuppressLint("ViewConstructor")
class PasscodeSetupPage(context: FloatingTutorialActivity) : TutorialPage(context) {

    private val nextButton: Button by lazy { findViewById<View>(R.id.tutorial_next_button) as Button }
    private val passcode: CodeInputView by lazy { findViewById<View>(R.id.code_input) as CodeInputView }
    private val passcodeSummary: TextView by lazy { findViewById<View>(R.id.passcode_summary) as TextView }

    override fun initPage() {
        setContentView(R.layout.page_passcode)
        setNextButtonText(R.string.set_passcode)
        passcodeSummary.setText(R.string.type_passcode)

        passcode.setShowKeyboard(true)
        passcode.inPasswordMode = false

        passcode.addOnCompleteListener { _ ->
            passcode.setEditable(true)
            passcode.clearError()
            passcode.setShowKeyboard(true)
            passcode.requestFocus()
        }

        nextButton.setOnClickListener {
            val code = passcode.code
            if (code.length == 4) {
                Settings.setValue(getActivity(), getActivity().getString(R.string.pref_secure_private_conversations), code)
                ApiUtils.updatePrivateConversationsPasscode(Account.accountId, code)

                getActivity().finishAnimated()
            }
        }
    }

    override fun onShown(firstTimeShown: Boolean) {
        super.onShown(firstTimeShown)
        passcode.requestFocus()
    }
}