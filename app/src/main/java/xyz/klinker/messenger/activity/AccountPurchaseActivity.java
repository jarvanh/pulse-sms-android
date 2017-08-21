package xyz.klinker.messenger.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.api.implementation.firebase.AnalyticsHelper;
import xyz.klinker.messenger.shared.data.ColorSet;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.util.DensityUtil;
import xyz.klinker.messenger.shared.util.billing.ProductAvailable;
import xyz.klinker.messenger.shared.util.billing.ProductType;

public class AccountPurchaseActivity extends AppCompatActivity {

    public static final String PRODUCT_ID_EXTRA = "product_id";

    private boolean isInitial = true;
    private boolean revealedPurchaseOptions = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AnalyticsHelper.accountTutorialStarted(this);

        setResult(Activity.RESULT_CANCELED);
        setContentView(R.layout.activity_account_purchase);
        setUpInitialLayout();

        Settings settings = Settings.get(this);
        if (settings.mainColorSet.color == Color.WHITE) {
            findViewById(R.id.initial_layout).setBackgroundColor(ColorSet.TEAL(this).color);
            findViewById(R.id.purchase_layout).setBackgroundColor(ColorSet.TEAL(this).color);
            ((TextView)findViewById(R.id.try_it))
                    .setTextColor(ColorStateList.valueOf(ColorSet.TEAL(this).color));
        } else {
            findViewById(R.id.initial_layout).setBackgroundColor(settings.mainColorSet.color);
            findViewById(R.id.purchase_layout).setBackgroundColor(settings.mainColorSet.color);
            ((TextView)findViewById(R.id.try_it))
                    .setTextColor(ColorStateList.valueOf(settings.mainColorSet.color));
        }

        new Handler().postDelayed(this::circularRevealIn, 100);
    }

    private void setUpInitialLayout() {
        findViewById(R.id.try_it).setOnClickListener(view -> tryIt());

        long startTime = 500;
        quickViewReveal(findViewById(R.id.icon_watch), startTime);
        quickViewReveal(findViewById(R.id.icon_tablet), startTime + 75);
        quickViewReveal(findViewById(R.id.icon_computer), startTime + 150);
        quickViewReveal(findViewById(R.id.icon_phone), startTime + 225);
        quickViewReveal(findViewById(R.id.icon_notify), startTime + 300);
    }

    protected void tryIt() {
        slidePurchaseOptionsIn();
        AnalyticsHelper.accountTutorialFinished(this);

        // set up purchasing views here
        View monthly = findViewById(R.id.monthly);
        View threeMonth = findViewById(R.id.three_month);
        View yearly = findViewById(R.id.yearly);
        View lifetime = findViewById(R.id.lifetime);

        if (!revealedPurchaseOptions) {
            long startTime = 300;
            quickViewReveal(yearly, startTime);
            quickViewReveal(threeMonth, startTime + 75);
            quickViewReveal(monthly, startTime + 150);
            quickViewReveal(lifetime, startTime + 225);

            revealedPurchaseOptions = true;
        }

        monthly.setOnClickListener(view -> warnOfPlayStoreSubscriptionProcess(ProductAvailable.createMonthly()));
        threeMonth.setOnClickListener(view -> warnOfPlayStoreSubscriptionProcess(ProductAvailable.createThreeMonth()));
        yearly.setOnClickListener(view -> warnOfPlayStoreSubscriptionProcess(ProductAvailable.createYearly()));
        lifetime.setOnClickListener(view -> finishWithPurchaseResult(ProductAvailable.createLifetime()));
    }

    private void finishWithPurchaseResult(ProductAvailable product) {
        Intent result = new Intent();
        result.putExtra(PRODUCT_ID_EXTRA, product.getProductId());
        setResult(Activity.RESULT_OK, result);

        if (product.getType().equals(ProductType.SUBSCRIPTION)) {
            Toast.makeText(this, R.string.subscription_toast, Toast.LENGTH_LONG).show();
        }

        AnalyticsHelper.accountSelectedPurchase(this);
        finish();
    }

    private void warnOfPlayStoreSubscriptionProcess(ProductAvailable product) {
        new AlertDialog.Builder(this, R.style.SubscriptionPicker)
                .setMessage(R.string.play_store_subscription_warning)
                .setPositiveButton(R.string.ok, (dialogInterface, i) -> finishWithPurchaseResult(product))
                .show();
    }

    private void circularRevealIn() {
        View view = findViewById(R.id.initial_layout);
        view.setVisibility(View.VISIBLE);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                int cx = view.getWidth() / 2;
                int cy = view.getHeight() / 2;
                float finalRadius = (float) Math.hypot(cx, cy);
                ViewAnimationUtils.createCircularReveal(view, cx, cy, 0, finalRadius).start();
            } else {
                view.setAlpha(0f);
                view.animate().alpha(1f).start();
            }
        } catch (Exception e) {
            finish();
        }
    }

    private void circularRevealOut() {
        final View view = findVisibleHolder();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int cx = view.getWidth() / 2;
            int cy = view.getHeight() / 2;
            float initialRadius = (float) Math.hypot(cx, cy);
            Animator anim =
                    ViewAnimationUtils.createCircularReveal(view, cx, cy, initialRadius, 0);
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    view.setVisibility(View.INVISIBLE);
                    close();
                }
            });

            anim.start();
        } else {
            view.animate().alpha(0f).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    close();
                }
            }).start();
        }
    }

    private void quickViewReveal(View view, long delay) {
        view.setTranslationX(-1 * DensityUtil.toDp(this, 16));
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);

        view.animate()
                .translationX(0)
                .alpha(1f)
                .setStartDelay(delay)
                .start();
    }

    private void slidePurchaseOptionsIn() {
        slideIn(findViewById(R.id.purchase_layout));
    }

    private void slideIn(View view) {
        isInitial = false;
        final View initial = findViewById(R.id.initial_layout);

        view.setVisibility(View.VISIBLE);
        view.setAlpha(0f);
        view.setTranslationX(view.getWidth());
        view.animate()
                .alpha(1f)
                .translationX(0)
                .setListener(null)
                .start();

        initial.animate()
                .alpha(0f)
                .translationX(-1 * initial.getWidth())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        initial.setVisibility(View.INVISIBLE);
                        initial.setTranslationX(0);
                    }
                }).start();
    }

    private void slideOut() {
        isInitial = true;
        final View visible = findVisibleHolder();
        View initial = findViewById(R.id.initial_layout);

        visible.animate()
                .alpha(0f)
                .translationX(visible.getWidth())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        visible.setVisibility(View.INVISIBLE);
                        visible.setTranslationX(0);
                    }
                }).start();

        initial.setVisibility(View.VISIBLE);
        initial.setAlpha(0f);
        initial.setTranslationX(-1 * initial.getWidth());
        initial.animate()
                .alpha(1f)
                .translationX(0)
                .setListener(null)
                .start();
    }

    private View findVisibleHolder() {
        View initial = findViewById(R.id.initial_layout);
        View purchase = findViewById(R.id.purchase_layout);

        if (initial.getVisibility() != View.INVISIBLE) {
            return initial;
        } else {
            return purchase;
        }
    }

    @Override
    public void onBackPressed() {
        if (isInitial) {
            circularRevealOut();
        } else {
            slideOut();
        }
    }

    protected void close() {
        finish();
        overridePendingTransition(0, 0);
    }
}
