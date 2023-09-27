package com.abdo.volumebooster;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.aeriustech.utils.BaseAdApplication;
import com.aeriustech.utils.GDPRUtils;
import com.aeriustech.utils.captcha.Captcha;
import com.aeriustech.utils.captcha.TextCaptcha;

import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;
import com.google.android.gms.ads.interstitial.InterstitialAd;

public class SplashActivity extends AppCompatActivity {
    private InterstitialAd mInterstitialAd;

    private static final int cRETRY_AD_TIMEOUT = 8000;
    private static final int cRETRY_AD_DELAY = 1000;
    private static long mTimerTick = 0;
    private Captcha c;
    View mCaptchaView = null;
    ProgressBar mProgress = null;
    private static final String TAG = "com.aadevelopers.SplashActivity";
    private ConsentInformation consentInformation;
    private ConsentRequestParameters params;
    private ImageView im;
    private EditText mEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);




        im = (ImageView) findViewById(R.id.captcha_img);
        mEdit = findViewById(R.id.captchaEdit);
        mCaptchaView = findViewById(R.id.captchaBox);
        mProgress = findViewById(R.id.splash_progress);
        mProgress.setVisibility(GONE);
        params = new ConsentRequestParameters
                .Builder()
                .build();
        SharedPreferences mPref = this.getSharedPreferences("BOOSTER", MODE_PRIVATE);
        String lcok = mPref.getString("COK", "0123456789abcdefghjklmopqrstuvwxyz");
        if ((!lcok.equals("codec-volume_mpeg") || BuildConfig.DEBUG) && BuildConfig.splash_captcha) {
            Button mOkBtn = findViewById(R.id.captcha_submit);
            mOkBtn.setEnabled(false);
            mCaptchaView.setVisibility(VISIBLE);
        } else {
            mCaptchaView.setVisibility(GONE);
            mProgress.setVisibility(VISIBLE);
        }
        requestConsent();
    }

    Runnable mLaunchLoop = new Runnable() {
        @Override
        public void run() {
            boolean lsuccess;
            if (BuildConfig.splash_appopen) {
                lsuccess = ((BaseAdApplication) SplashActivity.this.getApplication())
                        .showAppOpen(SplashActivity.this, () -> {
                            startActivity(new Intent(SplashActivity.this, MainActivity.class));
                            finish();
                        });
            } else {
                lsuccess = ((BaseAdApplication) SplashActivity.this.getApplication())
                        .showInterstitial(SplashActivity.this, () -> {
                            startActivity(new Intent(SplashActivity.this, MainActivity.class));
                            finish();
                        });
            }
            if (!lsuccess) {
                Log.i(TAG, "Ads failed to show");
                Log.i(TAG, "mTimerTick:" + mTimerTick);
                Log.i(TAG, "cRETRY_AD_TIMEOUT:" + cRETRY_AD_TIMEOUT);
                if (System.currentTimeMillis() - mTimerTick > cRETRY_AD_TIMEOUT) {
                    //give up
                    runOnUiThread(
                            () -> {
                                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                                finish();
                            });
                } else
                    // retry in a sec....
                    new Handler().postDelayed(this, cRETRY_AD_DELAY);
            }
        }
    };

    private void launchApp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCaptchaView.setVisibility(GONE);
                mProgress.setVisibility(VISIBLE);
            }
        });
        mTimerTick = System.currentTimeMillis();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((BaseAdApplication) getApplication()).initializeAdNetwork(
                        BuildConfig.hasAds,
                        getString(R.string.appopenid),
                        getString(R.string.interstitialid),
                        BuildConfig.minMSecsBetweenInter
                );
                mTimerTick = System.currentTimeMillis();
                new Handler().postDelayed(mLaunchLoop, 200);
            }
        });
    }

    public void continueLunch() {
        SharedPreferences mPref = this.getSharedPreferences("BOOSTER", MODE_PRIVATE);
        String lcok = mPref.getString("COK", "0123456789abcdefghjklmopqrstuvwxyz");
        if ((!lcok.equals("codec-volume_mpeg") || BuildConfig.DEBUG) && BuildConfig.splash_captcha) {
            Button mOkBtn = findViewById(R.id.captcha_submit);
            mOkBtn.setEnabled(false);
            mCaptchaView.setVisibility(VISIBLE);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    float scale = SplashActivity.this.getResources().getDisplayMetrics().density;
                    int height = Math.round(50 * scale);
                    c = new TextCaptcha(im.getMeasuredWidth(), height, 5, TextCaptcha.TextOptions.NUMBERS_AND_LETTERS);
                    im.setImageBitmap(c.image);
                    im.setLayoutParams(new LinearLayout.LayoutParams(c.width * 2, c.height * 2));

                    mOkBtn.setEnabled(true);
                    final int[] attempt = {0};
                    mOkBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (mEdit.getText().toString().toLowerCase().equals(c.answer.trim().toLowerCase())) {
                                mPref.edit().putString("COK", "codec-volume_mpeg").apply();
                                mOkBtn.setEnabled(false);
                                launchApp();
                            } else {
                                Toast.makeText(SplashActivity.this, "WRONG VALUE!", Toast.LENGTH_SHORT).show();
                                attempt[0]++;
                                if (attempt[0] > 5) {
                                    mOkBtn.setEnabled(false);
                                    finish();
                                    android.os.Process.killProcess(android.os.Process.myPid());
                                } else {
                                    mOkBtn.setEnabled(true);
                                }
                            }
                        }
                    });
                }
            });
        } else {
            launchApp();
        } //launchApp();
    }

    public void requestConsent() {
        consentInformation = UserMessagingPlatform.getConsentInformation(this);
        boolean result = consentInformation.canRequestAds();
        Log.i(TAG, String.valueOf(result));
        if (!result) {
            consentInformation.requestConsentInfoUpdate(
                    this,
                    params,
                    (ConsentInformation.OnConsentInfoUpdateSuccessListener) () -> {
                        UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                                this,
                                (ConsentForm.OnConsentFormDismissedListener) loadAndShowError -> {
                                    if (loadAndShowError != null) {
                                        // Consent gathering failed.
                                        Log.w(TAG, String.format("%s: %s", loadAndShowError.getErrorCode(), loadAndShowError.getMessage()));
                                    }
                                    // Consent has been gathered.
                                    if (new GDPRUtils().canShowPersonalizedAds(SplashActivity.this)) {
                                        Log.i(TAG, "CONSENTED");
                                    } else {
                                        Log.i(TAG, "NOT CONSENTED");
                                    }
                                    continueLunch();
                                }
                        );
                    },
                    (ConsentInformation.OnConsentInfoUpdateFailureListener) requestConsentError -> {
                        // Consent gathering failed.
                        Log.w(TAG, String.format("%s: %s",
                                requestConsentError.getErrorCode(),
                                requestConsentError.getMessage()));
                        requestConsent();
                    });
        } else {
            continueLunch();
        }
    }
}