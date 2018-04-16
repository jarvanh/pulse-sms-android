package xyz.klinker.messenger.shared.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.github.ajalt.reprint.core.Reprint
import xyz.klinker.android.floating_tutorial.FloatingTutorialActivity
import xyz.klinker.android.floating_tutorial.TutorialPage
import com.github.ajalt.reprint.core.AuthenticationFailureReason
import com.github.ajalt.reprint.core.AuthenticationListener
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.Settings


class PasswordVerificationActivity : FloatingTutorialActivity(), AuthenticationListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(Activity.RESULT_CANCELED)
        if (Reprint.hasFingerprintRegistered()) {
            Reprint.authenticate(this)
        }
    }

    override fun onSuccess(moduleTag: Int) {
        setResult(Activity.RESULT_OK)
        finishAnimated()
    }

    override fun onFailure(failureReason: AuthenticationFailureReason, fatal: Boolean,
                           errorMessage: CharSequence, moduleTag: Int, errorCode: Int) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
    }

    override fun getPages(): List<TutorialPage> {
        return if (Reprint.hasFingerprintRegistered()) {
            listOf(FingerprintPage(this), PasscodePage(this))
        } else {
            listOf(PasscodePage(this))
        }
    }

    companion object {
        const val REQUEST_CODE = 185
    }
}

@SuppressLint("ViewConstructor")
class FingerprintPage(context: FloatingTutorialActivity) : TutorialPage(context) {
    override fun initPage() {
        setContentView(R.layout.page_fingerprint)

        setNextButtonText(R.string.passcode)
        setBackgroundColor(Settings.mainColorSet.color)
        setNextButtonTextColor(Color.WHITE)
        setProgressIndicatorColor(Color.WHITE)
    }
}

@SuppressLint("ViewConstructor")
class PasscodePage(context: FloatingTutorialActivity) : TutorialPage(context) {

    private val nextButton: Button by lazy { getActivity().findViewById<View>(R.id.tutorial_next_button) as Button }

    override fun initPage() {
        setContentView(R.layout.page_fingerprint)
        setNextButtonText(R.string.verify)
    }

    override fun onShown(firstTimeShown: Boolean) {
        super.onShown(firstTimeShown)

        if (!firstTimeShown) {
            return
        }

        nextButton.setOnClickListener {
            setActivityResult(Activity.RESULT_OK)
        }
    }
}