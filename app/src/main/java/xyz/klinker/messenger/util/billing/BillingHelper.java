package xyz.klinker.messenger.util.billing;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.android.vending.billing.IInAppBillingService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import xyz.klinker.messenger.R;

public class BillingHelper {

    private static final int REQUEST_PURCHASE = 1001;
    private static final String DEVELOPER_PAYLOAD = "bGoa+V7g/yqDXvKRqq+JTFn4uQZbPiQJo4pf9RzJ";

    private Context context;
    private IInAppBillingService billingService;
    private PurchasedItemCallback purchaseCallback;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            billingService = IInAppBillingService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            billingService = null;
        }
    };

    public BillingHelper(Context context) {
        this.context = context;
        prepare();
    }

    private void prepare() {
        if (billingService == null) {
            Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
            serviceIntent.setPackage("com.android.vending");
            context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    public void destroy() {
        if (billingService != null) {
            context.unbindService(serviceConnection);
        }
    }

    public boolean isPrepared() {
        return billingService != null;
    }

    public boolean hasPurchasedProduct() {
        return queryAllPurchasedProducts().size() > 0;
    }

    private void waitOnServiceInitialization() {
        int i = 0;
        while (!isPrepared() && i < 10) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) { }

            i++;
        }
    }

    public List<ProductAvailable> queryAllAvailableProducts() {
        waitOnServiceInitialization();

        List<ProductAvailable> products = new ArrayList<>();
        products.addAll(queryAvailableProducts(ProductType.SINGLE_PURCHASE));
        products.addAll(queryAvailableProducts(ProductType.SUBSCRIPTION));

        return products;
    }

    private List<ProductAvailable> queryAvailableProducts(ProductType type) {
        Bundle querySkus = new Bundle();
        ArrayList<String> productIds = new ArrayList<>();
        productIds.addAll(Arrays.asList(type.getAvailableProductIds()));
        querySkus.putStringArrayList("ITEM_ID_LIST", productIds);

        try {
            return ProductAvailable.createFromBundle(type,
                    billingService.getSkuDetails(3, context.getPackageName(), type.getIdentifier(), querySkus));
        } catch (RemoteException e) {
            return new ArrayList<>();
        }
    }

    public List<ProductPurchased> queryAllPurchasedProducts() {
        waitOnServiceInitialization();

        List<ProductPurchased> products = new ArrayList<>();
        products.addAll(queryPurchasedProducts(ProductType.SINGLE_PURCHASE));
        products.addAll(queryPurchasedProducts(ProductType.SUBSCRIPTION));

        return products;
    }

    private List<ProductPurchased> queryPurchasedProducts(ProductType type) {
        try {
            return ProductPurchased.createFromBundle(type,
                    billingService.getPurchases(3, context.getPackageName(), type.getIdentifier(), null));
        } catch (RemoteException e) {
            return new ArrayList<>();
        }
    }

    public void purchaseItem(Activity activity, ProductAvailable productAvailable, PurchasedItemCallback callback) {
        this.purchaseCallback = callback;
        waitOnServiceInitialization();

        try {
            Bundle buyIntentBundle = billingService.getBuyIntent(3, context.getPackageName(),
                    productAvailable.getProductId(), productAvailable.getType().getIdentifier(), DEVELOPER_PAYLOAD);
            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
            activity.startIntentSenderForResult(pendingIntent.getIntentSender(),
                    REQUEST_PURCHASE, new Intent(), Integer.valueOf(0), Integer.valueOf(0),
                    Integer.valueOf(0));
        } catch (Exception e) {

        }
    }

    public boolean handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PURCHASE) {
            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");

            if (resultCode == Activity.RESULT_OK) {
                try {
                    JSONObject jo = new JSONObject(purchaseData);
                    String purchasedProductId = jo.getString("productId");
                    String developerPayload = jo.getString("developerPayload");

                    if (developerPayload.equals(DEVELOPER_PAYLOAD) && purchaseCallback != null) {
                        purchaseCallback.onItemPurchased(purchasedProductId);
                    } else {
                        purchaseCallback.onPurchaseError(context.getString(R.string.purchase_error_bad_sku));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    purchaseCallback.onPurchaseError(context.getString(R.string.purchase_error_bad_google_play));
                }
            } else {
                purchaseCallback.onPurchaseError(context.getString(R.string.purchase_error_cancelled));
            }

            return true;
        } else {
            return false;
        }
    }
}
