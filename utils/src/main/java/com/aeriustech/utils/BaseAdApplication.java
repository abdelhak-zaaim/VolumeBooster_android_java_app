package com.aeriustech.utils;

import android.app.Activity;
import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import com.aeriustech.utils.admob.AppOpenManager;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class BaseAdApplication extends Application {

    protected static final AtomicReference<AppOpenManager> mAppOpenManager=new AtomicReference<>();
    protected static final AtomicReference<BaseInterstitialManager> mInterManager=new AtomicReference<>();


//returns false if appopen ad didn't finish loading.  returns true if ad is ready, and will execute aSuccess runnable when ad is closed.

    public boolean showAppOpen(Activity aActivity, Runnable aSuccess){
        if (mAdsEnabled) {
            if (mAppOpenManager.get() != null) {
                mAppOpenManager.get().setCurrentActivity(aActivity);
                return mAppOpenManager.get().showAdIfAvailable(aSuccess);
            } else {
                return false;
            }
        }else{
            aSuccess.run();
            return true;
        }
    }

    private boolean mAdsEnabled =true;

    public boolean showInterstitial(Activity aAct, Runnable aSuccess){
        if (mAdsEnabled){
            if (mInterManager.get()!=null){
                return mInterManager.get().showInterstitial(aAct,aSuccess);
            }else{
                return false;
            }
        }else{
            aSuccess.run();
            return true;
        }
    }


    public void loadNativeBanner(Activity aAct, ViewGroup aContainerView, String aNativeBannerID) throws Exception {
        if (mAdsEnabled) {
            if (mInterManager.get() != null) {
                mInterManager.get().loadNativeBanner(aAct,aContainerView,aNativeBannerID);
            }
        }else{
            aContainerView.setVisibility(View.INVISIBLE);
        }
    }

    public void loadBanner(Activity aAct,View aView, String aBannerID){
        if (mAdsEnabled) {
            if (mInterManager.get() != null) {
                mInterManager.get().loadBanner(aAct,aView,aBannerID);
            }
        }
    }


    private final AtomicBoolean mInitialized = new AtomicBoolean(false);

    protected interface InitCallback{
        void onDone();
    }

    // implement here to create the AppOpenManager & BaseInterstitialManager derivative object instances.  Put the instances in the mAppOpenManager and mInterMnager datamembers.
    protected abstract void doInitializeAdNetwork(String aAppOpenID,
                                                  String aInterID,
                                                  int aMinMsecBetweenInters,
                                                  InitCallback aCb);

    // IMPORTANT: This needs to be called on UI thread from splash constructor.
    public void initializeAdNetwork(boolean aHasAds,
                                    String aAppOpenID,
                                    String aInterID,
                                    int aMinMsecBetweenInters){
        mAdsEnabled=aHasAds;
        if (aHasAds && !mInitialized.get()) {
            doInitializeAdNetwork(aAppOpenID, aInterID, aMinMsecBetweenInters, new InitCallback() {
                @Override
                public void onDone() {
                    mInitialized.set(true);
                }
            });
        }
    }

}
