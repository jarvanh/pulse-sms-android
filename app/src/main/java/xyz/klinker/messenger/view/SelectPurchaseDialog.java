package xyz.klinker.messenger.view;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.util.billing.ProductAvailable;

public class SelectPurchaseDialog extends AlertDialog.Builder {

    public interface PurchaseSelectedListener {
        void onPurchaseSelected(ProductAvailable product);
    }

    private Context context;
    private PurchaseSelectedListener listener;

    private AlertDialog dialog;

    public SelectPurchaseDialog(Context context) {
        super(context);
        this.context = context;

        addView();
    }

    public SelectPurchaseDialog setPurchaseSelectedListener(PurchaseSelectedListener listener) {
        this.listener = listener;
        return this;
    }

    public void addView() {
        View root = LayoutInflater.from(context).inflate(R.layout.dialog_select_purchase, null);

        root.findViewById(R.id.lifetime).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectPurchase(ProductAvailable.createLifetime());
            }
        });

        root.findViewById(R.id.yearly).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectPurchase(ProductAvailable.createYearly());
            }
        });

        root.findViewById(R.id.three_months).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectPurchase(ProductAvailable.createThreeMonth());
            }
        });

        root.findViewById(R.id.monthly).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectPurchase(ProductAvailable.createMonthly());
            }
        });

        setView(root);
    }

    private void selectPurchase(ProductAvailable productAvailable) {
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
