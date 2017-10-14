package xyz.klinker.messenger.view;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.api.implementation.firebase.AnalyticsHelper;
import xyz.klinker.messenger.shared.util.billing.ProductAvailable;

public class SelectPurchaseDialog extends AlertDialog.Builder {

    public interface PurchaseSelectedListener {
        void onPurchaseSelected(ProductAvailable product);
    }

    private Context context;
    private PurchaseSelectedListener listener;

    private AlertDialog dialog;

    public SelectPurchaseDialog(Context context) {
        super(context, R.style.SubscriptionPicker);
        this.context = context;

        addView();
    }

    public SelectPurchaseDialog setPurchaseSelectedListener(PurchaseSelectedListener listener) {
        this.listener = listener;
        return this;
    }

    public void addView() {
        View root = LayoutInflater.from(context).inflate(R.layout.dialog_select_purchase, null);

        root.findViewById(R.id.lifetime).setOnClickListener(view ->
                selectPurchase(ProductAvailable.Companion.createLifetime()));

        root.findViewById(R.id.yearly).setOnClickListener(view ->
                selectPurchase(ProductAvailable.Companion.createYearly()));

        root.findViewById(R.id.three_months).setOnClickListener(view ->
                selectPurchase(ProductAvailable.Companion.createThreeMonth()));

        root.findViewById(R.id.monthly).setOnClickListener(view ->
                selectPurchase(ProductAvailable.Companion.createMonthly()));

        setView(root);
    }

    private void selectPurchase(ProductAvailable productAvailable) {
        AnalyticsHelper.accountSelectedPurchase(getContext());

        if (listener != null) {
            listener.onPurchaseSelected(productAvailable);
        }

        if (dialog != null) {
            dialog.dismiss();
        }
    }

    @Override
    public AlertDialog show() {
        dialog = super.show();
        return dialog;
    }
}
