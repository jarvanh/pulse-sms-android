package xyz.klinker.messenger.activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.ViewAnimationUtils

import xyz.klinker.messenger.R
import xyz.klinker.messenger.api.implementation.firebase.AnalyticsHelper
import xyz.klinker.messenger.fragment.settings.MyAccountFragment
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.util.DensityUtil

class AccountTrialActivity : AppCompatActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AnalyticsHelper.accountStartTrialTutorial(this)

        setResult(Activity.RESULT_CANCELED)
        setContentView(R.layout.activity_account_trial)
        setUpInitialLayout()

        if (Settings.mainColorSet.color == Color.WHITE) {
            findViewById<View>(R.id.initial_layout).setBackgroundColor(ColorSet.TEAL(this).color)
        } else {
            findViewById<View>(R.id.initial_layout).setBackgroundColor(Settings.mainColorSet.color)
        }

        Handler().postDelayed({ this.circularRevealIn() }, 100)
    }

    private fun setUpInitialLayout() {
        findViewById<View>(R.id.try_it).setOnClickListener { tryIt() }

        val startTime: Long = 500
        quickViewReveal(findViewById(R.id.icon_watch), startTime)
        quickViewReveal(findViewById(R.id.icon_tablet), startTime + 75)
        quickViewReveal(findViewById(R.id.icon_computer), startTime + 150)
        quickViewReveal(findViewById(R.id.icon_phone), startTime + 225)
        quickViewReveal(findViewById(R.id.icon_notify), startTime + 300)
    }

    private fun tryIt() {
        setResult(MyAccountFragment.RESULT_START_TRIAL)
        AnalyticsHelper.accountAcceptFreeTrial(this)

        close()
    }

    private fun circularRevealIn() {
        val view = findViewById<View>(R.id.initial_layout)
        view.visibility = View.VISIBLE

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val cx = view.width / 2
                val cy = view.height / 2
                val finalRadius = Math.hypot(cx.toDouble(), cy.toDouble()).toFloat()
                ViewAnimationUtils.createCircularReveal(view, cx, cy, 0f, finalRadius).start()
            } else {
                view.alpha = 0f
                view.animate().alpha(1f).start()
            }
        } catch (e: Exception) {
            finish()
        }
    }

    private fun circularRevealOut() {
        val view = findVisibleHolder()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val cx = view.width / 2
            val cy = view.height / 2
            val initialRadius = Math.hypot(cx.toDouble(), cy.toDouble()).toFloat()
            val anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, initialRadius, 0f)
            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    view.visibility = View.INVISIBLE
                    close()
                }
            })

            anim.start()
        } else {
            view.animate().alpha(0f).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    close()
                }
            }).start()
        }
    }

    private fun quickViewReveal(view: View, delay: Long) {
        view.translationX = (-1 * DensityUtil.toDp(this, 16)).toFloat()
        view.alpha = 0f
        view.visibility = View.VISIBLE

        view.animate()
                .translationX(0f)
                .alpha(1f)
                .setStartDelay(delay)
                .start()
    }

    private fun findVisibleHolder(): View {
        val initial = findViewById<View>(R.id.initial_layout)
        val purchase = findViewById<View>(R.id.purchase_layout)

        return if (initial.visibility != View.INVISIBLE) {
            initial
        } else {
            purchase
        }
    }

    override fun onBackPressed() {
        circularRevealOut()
    }

    private fun close() {
        finish()
        overridePendingTransition(0, 0)
    }

    companion object {

    }
}
