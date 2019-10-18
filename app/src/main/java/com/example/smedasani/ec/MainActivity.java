package com.example.smedasani.ec;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.PayPal;
import com.braintreepayments.api.exceptions.BraintreeError;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.PayPalAccountNonce;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.PostalAddress;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;
// Braintree link https://developers.braintreepayments.com/guides/client-sdk/setup/android/v2#browser-switch-setup
public class MainActivity extends AppCompatActivity implements PaymentMethodNonceCreatedListener,BraintreeCancelListener,BraintreeErrorListener {

    private String clientToken="";
    private String nonce;
    private BraintreeFragment mBraintreeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("Suresh******  ","onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Log.d("Suresh******  exit  ","onCreate");
    }
    public void payNow(View v){
        Log.d("Suresh******  enter  ","payNow");
        final Activity activity=this;

        AsyncHttpClient client = new AsyncHttpClient();
        client.get("https://ppbraintree.herokuapp.com/client_token_sb", new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
            }
            @Override
            public void onSuccess(int statusCode, Header[] headers, String response) {
                Log.i("Suresh******Token",response);
                clientToken = response;
                try {
                    mBraintreeFragment = BraintreeFragment.newInstance(activity, clientToken);
                    Log.i("Suresh******  mBrain", "done");
                } catch (InvalidArgumentException e) {
                    // There was an issue with your authorization string.
                    Log.e("Suresh******  mBrain",e.toString());

                }
                setupBraintreeAndStartExpressCheckout();
                // setupBraintreeAndStartExpressCheckout();
                final TextView tvOut = (TextView) findViewById(R.id.textView);
              //  tvOut.setText("pay success");

            }
        });

        Log.d("Suresh******  exit  ","payNow");
    }

    @Override
    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        Log.d("Suresh  ","onPaymentMethodNonceCreated");
        // Send nonce to server
        String nonce = paymentMethodNonce.getNonce();
        Log.i("Suresh nonce",nonce);
        if (paymentMethodNonce instanceof PayPalAccountNonce) {
            PayPalAccountNonce payPalAccountNonce = (PayPalAccountNonce)paymentMethodNonce;

            // Access additional information
            String email = payPalAccountNonce.getEmail();
            String firstName = payPalAccountNonce.getFirstName();
            String lastName = payPalAccountNonce.getLastName();
            String phone = payPalAccountNonce.getPhone();

            // See PostalAddress.java for details
            PostalAddress billingAddress = payPalAccountNonce.getBillingAddress();
            PostalAddress shippingAddress = payPalAccountNonce.getShippingAddress();
        }
        Log.i("Suresh******  nonce  ",nonce);
        postNonceToServer(nonce);
        Log.i("Suresh******  exit  ","onPaymentMethodNonceCreated");
    }

    private void postNonceToServer(String nonce) {
        Log.i("Suresh******  enter  ","postNonceToServer");
        Log.i("Suresh******  nonce  ",nonce);
        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
     /*   params.setForceMultipartEntityContentType(true);
        params.setContentEncoding("application/x-www-form-urlencoded");*/
        params.put("nonce", nonce);
      //  params.put("amount1", "20");
        Log.d("nonce  ",nonce);
       // client.post("https://ecbtserver.cfapps.io/checkout", params,
        client.post("https://ppbraintree.herokuapp.com/checkout_sb", params,
                new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        String body = new String(responseBody);

                        Log.d("payment  ",body);
                        final TextView tvOut = (TextView) findViewById(R.id.textView);

                            tvOut.setText(body);


                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        Log.e("checkout  ",error.getMessage());
                    }
                    // Your implementation here
                }
        );
        Log.d("Suresh******  exit  ","postNonceToServer");
    }

    private void setupBraintreeAndStartExpressCheckout() {
        Log.d("Suresh******  enter  ","setupBraintreeAndStartExpressCheckout");
        PayPalRequest request = new PayPalRequest("10")
                .currencyCode("USD")
                .intent(PayPalRequest.INTENT_SALE);

        PayPal.requestOneTimePayment(mBraintreeFragment, request);
        Log.d("Suresh******  exit  ","setupBraintreeAndStartExpressCheckout");
    }

    @Override
    public void onError(Exception error) {
        if (error instanceof ErrorWithResponse) {
            ErrorWithResponse errorWithResponse = (ErrorWithResponse) error;
            BraintreeError cardErrors = errorWithResponse.errorFor("creditCard");
            if (cardErrors != null) {
                // There is an issue with the credit card.
                BraintreeError expirationMonthError = cardErrors.errorFor("expirationMonth");
                if (expirationMonthError != null) {
                    // There is an issue with the expiration month.
                    Log.d("Suresh******  exit  ","setupBraintreeAndStartExpressCheckout");
                    Toast.makeText(this,expirationMonthError.getMessage(),Toast.LENGTH_LONG);
                   //setErrorMessage(expirationMonthError.getMessage());
                }
            }
        }
    }

    @Override
    public void onCancel(int requestCode) {
        // Use this to handle a canceled activity, if the given requestCode is important.
        // You may want to use this callback to hide loading indicators, and prepare your UI for input
    }

}
