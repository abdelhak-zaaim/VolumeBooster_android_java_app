package com.aeriustech.utils.admob;


import android.app.Activity;
import android.app.Application;
import android.nfc.tech.TagTechnology;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.aeriustech.utils.GDPRUtils;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

//only create this class after admobile initialized!
public class AppOpenManager implements DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks  {

    private static final String TAG = "com.aadevelopers.AppOpenManager";
    //  private static final String AD_UNIT_ID = // "ca-app-pub-3560277835764465/2086808337"; //"ca-app-pub-3560277835764465/2086808337";
    private AppOpenAd appOpenAd = null;
    private Activity currentActivity;
    private long loadTime = 0;
    private AppOpenAd.AppOpenAdLoadCallback loadCallback;

    private final Application mApplication;
    private static boolean isShowingAd = false;
    private boolean mLoading=false;
    private final String mAppOpenAdID;
    private AtomicInteger mAttempts=new AtomicInteger(0);
    private int maxAttempts=2;
    /** Constructor */
    public AppOpenManager(Application myApplication, String aAppOpenAdID) {
        this.mAppOpenAdID=aAppOpenAdID;
        this.mApplication = myApplication;
        this.mApplication.registerActivityLifecycleCallbacks(this);
        try {
            fetchAd();
        }catch (Exception E){
            E.printStackTrace();
        }
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }



    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStart(owner);
        showAdIfAvailable(null);
        Log.d(TAG, "onStart");
    }

    /*
    @OnLifecycleEvent(ON_START)
    public void onStart() {
        showAdIfAvailable(null);
        Log.d(LOG_TAG, "onStart");
    }
     */

    /** Request an ad */
    public void fetchAd() {
        // We will implement this below.
        Log.i(TAG,"Fetching app open add");
        if (!mLoading)
            Log.i(TAG,"App open ad is not loading");
        try {
            if (isAdAvailable()) {
                    return;
                }
                AppOpenManager.this.mLoading=true;
                loadCallback = new AppOpenAd.AppOpenAdLoadCallback() {

                            @Override
                            public void onAdLoaded(@NonNull AppOpenAd appOpenAd) {
                                super.onAdLoaded(appOpenAd);
                                AppOpenManager.this.mLoading=false;
                                AppOpenManager.this.appOpenAd = appOpenAd;
                                AppOpenManager.this.loadTime = (new Date()).getTime();
                                mAttempts.set(0);
                            }

                            @Override
                            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                                AppOpenManager.this.mLoading=false;
                                super.onAdFailedToLoad(loadAdError);
                            }
                        };
            Log.i(TAG,"Requesting app open:"+mAttempts.get());
            Bundle extras = new Bundle();
            extras.putString("npa", "1"); // "1" for non-personalized ads
            AdRequest request = null;
            if (new GDPRUtils().canShowPersonalizedAds(mApplication.getApplicationContext())){
                Log.i(TAG,"CONSENTED");
                request = new AdRequest.Builder()
                        .build();
            }else{
                Log.i(TAG,"NOT CONSENTED");
                request = new AdRequest.Builder()
                        .addNetworkExtrasBundle(AdMobAdapter.class, extras)
                        .build();
            }

            if (mAttempts.get()<=maxAttempts){
                    AppOpenAd.load(mApplication,  mAppOpenAdID , request, AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, loadCallback);
                }else{
                    Log.i(TAG,"Giving up loading app open"+mAttempts.get());
                }
            }catch (Exception E){
                E.printStackTrace();
                mLoading=false;
            }
    }

    /** Shows the ad if one isn't already showing. */
    public boolean showAdIfAvailable(Runnable aSuccess) {
        // Only show ad if there is not already an app open ad currently showing
        // and an ad is available.
        if (!mLoading && !isShowingAd && isAdAvailable()) {
            FullScreenContentCallback fullScreenContentCallback =
                    new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            // Set the reference to null so isAdAvailable() returns false.
                            if (aSuccess!=null)
                                try {
                                    aSuccess.run();
                                }catch (Exception E){
                                    E.printStackTrace();
                                }
                            AppOpenManager.this.appOpenAd = null;
                            isShowingAd = false;
                            fetchAd();
                        }
                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {
                            if (aSuccess!=null)
                                try {
                                    aSuccess.run();
                                }catch (Exception E){
                                    E.printStackTrace();
                                }
                        }
                        @Override
                        public void onAdShowedFullScreenContent() {
                            isShowingAd = true;
                        }
                    };
            appOpenAd.setFullScreenContentCallback(fullScreenContentCallback);
            appOpenAd.show(currentActivity);
            return true;
        } else {
            Log.d(TAG, "Can not show ad.");
            fetchAd();
            return false;
        }
    }
    /** Creates and returns ad request. */
    private AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }
    /** Utility method to check if ad was loaded more than n hours ago. */
    private boolean wasLoadTimeLessThanNHoursAgo(long numHours) {
        long dateDifference = (new Date()).getTime() - this.loadTime;
        long numMilliSecondsPerHour = 3600000;
        return (dateDifference < (numMilliSecondsPerHour * numHours));
    }

    /** Utility method that checks if ad exists and can be shown. */
    public boolean isAdAvailable() {
        return appOpenAd != null;
    }
    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        currentActivity = activity;
    }
    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        currentActivity = activity;
    }
    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivity = activity;
    }
    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }
    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }
    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }
    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        currentActivity = null;
    }

    public void setCurrentActivity(Activity aActivity) {
        currentActivity=aActivity;
    }
}
