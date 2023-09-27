package com.aeriustech.utils.admob;

import com.aeriustech.utils.BaseAdApplication;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;

import java.util.Arrays;
import java.util.List;

public class AdmobApplication extends BaseAdApplication {

    // IMPORTANT: This needs to be called on UI thread from splash constructor.
    @Override
    public void doInitializeAdNetwork(String aAppOpenID, String aInterID, int aMinMsecBetweenInters, InitCallback aCb){
        MobileAds.initialize(this, initializationStatus -> {
            aCb.onDone();
            mAppOpenManager.set( new AppOpenManager(AdmobApplication.this, aAppOpenID));
            mInterManager.set(new AdmobInterstitialManager(AdmobApplication.this, aInterID,  aMinMsecBetweenInters));
        });
    }

}
