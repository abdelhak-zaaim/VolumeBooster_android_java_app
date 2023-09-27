package com.aeriustech.utils.applovin;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.aeriustech.utils.BaseAdApplication;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.facebook.ads.AdSettings;

public class AppLovinApplication extends BaseAdApplication implements DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks{


    public Activity currentActivity;

    @Override
    protected void doInitializeAdNetwork(String aAppOpenID, String aInterID,  int aMinMsecBetweenInters, InitCallback aCb) {
        // Make sure to set the mediation provider value to "max" to ensure proper functionality
        AppLovinSdk.getInstance( this ).setMediationProvider( "max" );
        AdSettings.setDataProcessingOptions( new String[] {"LDU"}, 1, 1000 );
        AppLovinSdk.initializeSdk( AppLovinApplication.this, new AppLovinSdk.SdkInitializationListener() {
            @Override
            public void onSdkInitialized(final AppLovinSdkConfiguration configuration)
            {
                mInterManager.set(new AppLovinInterstitialManager(AppLovinApplication.this, aInterID, aMinMsecBetweenInters));
                aCb.onDone();
                // AppLovin SDK is initialized, start loading ads
            }
        } );

        registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }



    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
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
        currentActivity = null;
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        currentActivity = null;
    }
}
