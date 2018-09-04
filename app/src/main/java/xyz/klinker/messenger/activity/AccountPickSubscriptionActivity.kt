package xyz.klinker.messenger.activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.TextView

import xyz.klinker.messenger.R
import xyz.klinker.messenger.api.implementation.firebase.AnalyticsHelper
import xyz.klinker.messenger.fragment.settings.MyAccountFragment
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.util.DensityUtil
import xyz.klinker.messenger.shared.util.billing.ProductAvailable
import xyz.klinker.messenger.shared.util.billing.ProductType

class AccountPickSubscriptionActivity : AppCompatActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AnalyticsHelper.accountTutorialStarted(this)

        setResult(Activity.RESULT_CANCELED)
        setContentView(R.layout.activity_account_pick_subscription)
        setUpPurchaseLayout()

        if (Settings.mainColorSet.color == Color.WHITE) {
            findViewById<View>(R.id.purchase_layout).setBackgroundColor(ColorSet.TEAL(this).color)
        } else {
            findViewById<View>(R.id.purchase_layout).setBackgroundColor(Settings.mainColorSet.color)
        }

        Handler().postDelayed({ this.circularRevealIn() }, 100)
    }

    private fun setUpPurchaseLayout() {
        AnalyticsHelper.accountTutorialFinished(this)

        // set up purchasing views here
        val monthly = findViewById<View>(R.id.monthly)
        val threeMonth = findViewById<View>(R.id.three_month)
        val yearly = findViewById<View>(R.id.yearly)
        val lifetime = findViewById<View>(R.id.lifetime)
        val cancelTrial = findViewById<View>(R.id.cancel_trial)
        val signIn = findViewById<View>(R.id.sign_in)

        val startTime: Long = 300
        quickViewReveal(yearly, startTime)
        quickViewReveal(threeMonth, startTime + 75)
        quickViewReveal(monthly, startTime + 150)
        quickViewReveal(lifetime, startTime + 225)

        monthly.setOnClickListener { warnOfPlayStoreSubscriptionProcess(ProductAvailable.createMonthlyNoTrial()) }
        threeMonth.setOnClickListener { warnOfPlayStoreSubscriptionProcess(ProductAvailable.createThreeMonthNoTrial()) }
        yearly.setOnClickListener { warnOfPlayStoreSubscriptionProcess(ProductAvailable.createYearlyNoTrial()) }
        lifetime.setOnClickListener { finishWithPurchaseResult(ProductAvailable.createLifetime()) }
        cancelTrial.setOnClickListener { finishWithTrialCancellation() }
        signIn.setOnClickListener { startSignIn() }

        if (intent.getBooleanExtra(ARG_CHANGING_SUBSCRIPTION, false)) {
            signIn.visibility = View.INVISIBLE
        } else {
            findViewById<View>(R.id.buttons).visibility = View.VISIBLE
        }

        if (intent.getBooleanExtra(ARG_FREE_TRIAL, false)) {
            quickViewReveal(cancelTrial, startTime + 300)

            cancelTrial.visibility = View.VISIBLE
            signIn.visibility = View.INVISIBLE

            findViewById<TextView>(R.id.select_purchase_title).setText(R.string.thanks_for_trying)
            findViewById<View>(R.id.buttons).visibility = View.GONE
        }
    }

    private fun finishWithPurchaseResult(product: ProductAvailable) {
        val result = Intent()
        result.putExtra(PRODUCT_ID_EXTRA, product.productId)
        setResult(Activity.RESULT_OK, result)

        if (product.type == ProductType.SUBSCRIPTION) {
//            Toast.makeText(this, R.string.subscription_toast, Toast.LENGTH_LONG).show()
        }

        AnalyticsHelper.accountSelectedPurchase(this)
        finish()
    }

    private fun finishWithTrialCancellation() {
        AnalyticsHelper.accountFreeTrialUpgradeDialogCancelClicked(this)

        setResult(RESULT_CANCEL_TRIAL)
        finish()
    }

    private fun startSignIn() {
        setResult(MyAccountFragment.RESULT_SIGN_IN)
        AnalyticsHelper.accountSignInInsteadOfPurchase(this)

        finish()
    }

    private fun warnOfPlayStoreSubscriptionProcess(product: ProductAvailable) {
//        AlertDialog.Builder(this, R.style.SubscriptionPicker)
//                .setMessage(R.string.play_store_subscription_warning)
//                .setPositiveButton(R.string.ok) { _, _ -> finishWithPurchaseResult(product) }
//                .show()
        finishWithPurchaseResult(product)
    }

    private fun circularRevealIn() {
        val view = findViewById<View>(R.id.purchase_layout)
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
        val purchase = findViewById<View>(R.id.purchase_layout)
        return purchase
    }

    override fun onBackPressed() {
        circularRevealOut()
    }

    private fun close() {
        finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        val PRODUCT_ID_EXTRA = "product_id"
        val ARG_CHANGING_SUBSCRIPTION = "arg_changing_subscription"
        val ARG_FREE_TRIAL= "arg_free_trial"

        val RESULT_CANCEL_TRIAL = 33425
    }
}
