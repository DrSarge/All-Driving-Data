package com.example.jamessingleton.chffrapi;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.jamessingleton.chffrapi.com.examples.jamessingleton.chffrapi.data.Route;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.SignInButton;
import net.danlew.android.joda.JodaTimeAndroid;
import java.util.Map;


//Add to git!
public class MainActivity extends AppCompatActivity {

    Context mContext = MainActivity.this;
    private AccountManager mAccountManager;
    private AuthPreferences authPreferences;
    SharedPreferences sharedPref;
    static Map<String, Route> drives;
    static final String SCOPE = "https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/plus.me";
    private static final int AUTHORIZATION_CODE = 1993;
    private static final int ACCOUNT_CODE = 1601;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JodaTimeAndroid.init(this);
        setContentView(R.layout.activity_main);
        mAccountManager = AccountManager.get(this);
        authPreferences = new AuthPreferences(this);
        APIRequestsUtil.setAuthPreferences(authPreferences);
        sharedPref = getPreferences(Context.MODE_PRIVATE);
        final Context context = this;
        requestToken();
        System.out.println("Connection Preference: " + getPreferences(Context.MODE_PRIVATE).getString("connection", null));
        SignInButton signInButton = (SignInButton) findViewById(R.id.sign_in_button);
        TextView textView2 = (TextView) findViewById(R.id.textView2);
        ImageView imageView3 = (ImageView) findViewById(R.id.imageView3);
        boolean installed = appInstalledOrNot("ai.comma.chffr");
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isPreferedConnectionAvailable()) {
                    Toast.makeText(getApplicationContext(), "Preferred connection is available!", Toast.LENGTH_LONG).show();
                    if (authPreferences.getUser() != null && authPreferences.getToken() != null) {
                        System.out.println(authPreferences.getToken());

                        doCoolAuthenticatedStuff();

                        try {
                            APIRequestsUtil.run();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        Intent intent = new Intent(context, NavDrawerActivity.class);
                        startActivity(intent);

                    } else {
                        chooseAccount();
                    }
                }
                else {
                    Toast.makeText(getApplicationContext(), "You are not connected to your preferred connection." + "\n" +
                            "Please connect to your preferred connection: " + sharedPref.getString("connection",null) +" to continue."
                            , Toast.LENGTH_LONG).show();
                }

            }
        });

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        boolean dontShowDialog = sharedPref.getBoolean("DONT_SHOW_DIALOG", false);
        if (!dontShowDialog) {
            WifivsDataDialog myDiag = new WifivsDataDialog();
            myDiag.show(getFragmentManager(), "WiFi");
            myDiag.setCancelable(false);
        }
        if(installed) {
            //This intent will help you to launch if the package is already installed
            textView2.setText("This app is powered by data from Chffr.");
            imageView3.setVisibility(View.GONE);
            return;

        } else {
            imageView3.setVisibility(View.VISIBLE);
            textView2.setText("This app is powered by data from Chffr. You do not have Chffr installed on your phone, get Chffr now!");
            imageView3.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    String url = "https://play.google.com/store/apps/details?id=ai.comma.chffr&hl=en";
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                }
            });
        }

    }

    private boolean appInstalledOrNot(String uri) {
        PackageManager pm = getPackageManager();
        boolean app_installed;
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        }
        catch (PackageManager.NameNotFoundException e) {
            app_installed = false;
        }
        return app_installed;
    }

    private void doCoolAuthenticatedStuff() {
        Log.e("AuthApp", authPreferences.getToken());
    }

    private void chooseAccount() {
        Intent intent = AccountManager.newChooseAccountIntent(null, null, new String[]{"com.google"}, false, null, null, null, null);
        startActivityForResult(intent, ACCOUNT_CODE);
    }

    public void requestToken() {
        Account userAccount = null;
        String user = authPreferences.getUser();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        for (Account account : mAccountManager.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE)) {
            if (account.name.equals(user)) {
                userAccount = account;
                break;
            }
        }

        mAccountManager.getAuthToken(userAccount, "oauth2:" + SCOPE, null, this, new OnTokenAcquired(), null);
    }

    private void invalidateToken() {
        AccountManager mAccountManager = AccountManager.get(this);
        mAccountManager.invalidateAuthToken("com.google", authPreferences.getToken());
        authPreferences.setToken(null);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == AUTHORIZATION_CODE) {
                System.out.println(requestCode);
                requestToken();
            } else if (requestCode == ACCOUNT_CODE) {
                String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                authPreferences.setUser(accountName);

                // invalidate old tokens which might be cached. we want a fresh
                // one, which is guaranteed to work
                invalidateToken();

                requestToken();
            }
        }
    }

    public class OnTokenAcquired implements AccountManagerCallback<Bundle> {
        @Override
        public void run(AccountManagerFuture<Bundle> result) {
            try {
                Bundle bundle = result.getResult();
                Intent launch = (Intent) bundle.get(AccountManager.KEY_INTENT);
                if (launch != null) {
                    startActivityForResult(launch, AUTHORIZATION_CODE);
                } else {
                    String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);


                    authPreferences.setToken(token);
                    System.out.println(authPreferences);
                    doCoolAuthenticatedStuff();
                }
            } catch (Exception e) {
                Log.e("ERROR", e.getMessage(), e);
            }
        }
    }

    public boolean isPreferedConnectionAvailable(){
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if(ConnectionEnum.WIFI.value.equals(sharedPref.getString("connection", null))){
            NetworkInfo netInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if(netInfo != null && netInfo.isConnected())
                return true;
        }else if(ConnectionEnum.MOBILE.value.equals(sharedPref.getString("connection", null))){
            NetworkInfo netInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            if(netInfo != null && netInfo.isConnected())
                return true;
        }
        return false;
    }
}
