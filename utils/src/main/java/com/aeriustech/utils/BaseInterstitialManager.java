package com.aeriustech.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class BaseInterstitialManager{

    public int cMAX_INTERSTITIAL_LOAD_ATTEMPTS = 2;
    private long cINTERSTITIAL_TTL = 60*60000; //1 hour.  reload inter after.

    public int cMAX_BANNER_LOAD_ATTEMPTS = 2;  //0=retry forever

    private static final String TAG = "com.aadevelopers.BaseInterstitialManager";

    protected abstract boolean isInterstitialValid();
    protected abstract void doShowInterstitial(Activity aAct);
    protected abstract void DoLoadInterstitialAd(Context aAppContext, AdInterCallBack aCallback);

    boolean unmetered=false;

    protected interface AdInterCallBack {
        void onLoaded();
        void onFailed();
        void onAdDismissed();
        void onAdFailedToShow();
    }

    protected interface AdBannerCallBack {
        void onLoaded();
        void onFailed();
    }

    private AtomicInteger mAttempts=new AtomicInteger(0);
    private AtomicInteger mInterstitialAttempts=new AtomicInteger(0);

    private final AtomicBoolean mInterLoading= new AtomicBoolean(false);
    private long mInterstitialStamp=0;
    private Runnable mAfterInterstitial =null;
    private long mInterShownTick=0;

    protected String mAdID;
    protected Context mAppContext;
    protected int mMinMSecsBetweenInter;


/*
    ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
         //   resumeAllRequests();
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
         //   cancelAllRequests();
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);
            unmetered = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
        }
    };

*/


    public BaseInterstitialManager(Context aAppContext, String aAdID, int aMinMSecsBetweenInters){
        mAdID=aAdID;
        mMinMSecsBetweenInter=aMinMSecsBetweenInters;
        mAppContext = aAppContext;
/*
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();

        ConnectivityManager connectivityManager =
                (ConnectivityManager) aAppContext.getSystemService(ConnectivityManager.class);
        connectivityManager.requestNetwork(networkRequest, networkCallback);
*/
        loadInterstitial(aAppContext);
    }

    //returns false if interstitial didn't finish loading yet.  returns true if interstitial was ready and displayed, it will execute aSuccess runnable when ad is closed.
    public boolean showInterstitial(Activity aAct, Runnable aSuccess){
        try {
            if (!mInterLoading.get() && isInterstitialValid() && (System.currentTimeMillis()-mInterstitialStamp<cINTERSTITIAL_TTL)) {
                if (System.currentTimeMillis()-mInterShownTick<mMinMSecsBetweenInter){return false;}
                mAfterInterstitial=aSuccess;
                mInterShownTick =System.currentTimeMillis();
                doShowInterstitial(aAct);
                return true;
            } else {
                if (!mInterLoading.get()) { //load a new interstitial if its not still loading
                    loadInterstitial(aAct);
                }
                return false;  //operation failed,  either was already loading,  or interstitial in memory expired and need to load a new one.
            }
        }catch(Exception E){
            E.printStackTrace();
        }
        return false;
    }


    protected void loadInterstitial(Context aAppContext){
        Log.i(TAG,"Loading Interstitial ads");
        if (!mInterLoading.get()) {
            mInterLoading.set(true);
            DoLoadInterstitialAd(aAppContext, new AdInterCallBack() {

                @Override
                public void onLoaded() {
                    mInterLoading.set(false);
                    mInterstitialStamp=System.currentTimeMillis();
                    mInterstitialAttempts.set(0);
                }

                @Override
                public void onFailed() {
                    mInterstitialAttempts.getAndAdd(1);
                    //try again
//                    Log.i(TAG,"Interstitial ad failed to load:"+mInterstitialAttempts);
                    if (mInterstitialAttempts.get() <= cMAX_INTERSTITIAL_LOAD_ATTEMPTS) {
                        Log.i(TAG,"Interstitial ad retrying to load:"+mInterstitialAttempts);
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            public void run() {
                                mInterLoading.set(false);
                                loadInterstitial(mAppContext); //try again
                            }
                        }, 5000);
                    }else{
                        Log.i(TAG,"Giving up loading interstitial ads"+mInterstitialAttempts.get());
                        mInterLoading.set(false); //gave up.
                    }
                }

                @Override
                public void onAdDismissed() {
                    mInterstitialAttempts.set(0);
                    if (mAfterInterstitial !=null){
                        try {
                            mAfterInterstitial.run();
                        }catch (Exception E){
                            E.printStackTrace();
                        }
                        mAfterInterstitial=null;
                    }
                    //prepare next interstitial ( download another one )
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        public void run() {
                            mInterLoading.set(false);
                            loadInterstitial(mAppContext); //try again
                        }
                    }, 1000);
                }

                @Override
                public void onAdFailedToShow() {
                    mInterstitialAttempts.set(0);
                    if (mAfterInterstitial !=null){
                        try {
                            mAfterInterstitial.run();
                        }catch(Exception E){
                            E.printStackTrace();
                        }
                        mAfterInterstitial=null;
                    }
                }
            });
        }
    }




//this function needs to be called on the UI thread.
    public void loadBanner(Activity aAct,View aView, String aBannerID){
        Log.i(TAG,"Loading banner ads");
        if (!aAct.isFinishing() && !aAct.isDestroyed())
            doLoadBanner(aAct, aView, aBannerID, new BaseInterstitialManager.AdBannerCallBack() {

                @Override
                public void onLoaded() {
                    mAttempts.set(0);
                    aView.setVisibility(View.VISIBLE);
                }
                @Override
                public void onFailed() {
                    if (!aAct.isFinishing() && !aAct.isDestroyed()){
                        //keep trying...
                        mAttempts.getAndAdd(1);
                        Log.i(TAG,"Banner add failed:");
                        if (mAttempts.get() <=cMAX_BANNER_LOAD_ATTEMPTS) {
                            Log.i(TAG,"Banner add failed:Retrying:"+mAttempts);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                public void run() {
                                    if (!aAct.isFinishing() && !aAct.isDestroyed())
                                        loadBanner(aAct, aView,aBannerID); //try again
                                }
                            }, 5000);
                        }else{
//                            mAttempts =0;
//                            give up?
                            Log.i(TAG,"Banner add failed:Give up:"+mAttempts);
                        }
                    }
                }
            });
    }


    public void loadNativeBanner(Activity aAct, ViewGroup aContainerView, String aNativeBannerID) throws Exception {
//        if (!aAct.isFinishing() && !aAct.isDestroyed())
//            doLoadNativeBanner(aAct, aContainerView, aNativeBannerID, new BaseInterstitialManager.AdBannerCallBack() {
//                private int mAttempts =0;
//
//                @Override
//                public void onLoaded() {
//                    mAttempts=0;
//                    aContainerView.setVisibility(View.VISIBLE);
//                }
//
//                @Override
//                public void onFailed() {
//                    //keep trying...
//                    if (!aAct.isFinishing() && !aAct.isDestroyed()) {
//                        mAttempts++;
//                        if (cMAX_BANNER_LOAD_ATTEMPTS <= 0 || mAttempts < cMAX_BANNER_LOAD_ATTEMPTS) {
//                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//                                public void run() {
//                                    try {
//                                        if (!aAct.isFinishing() && !aAct.isDestroyed())
//                                            loadNativeBanner(aAct, aContainerView, aNativeBannerID); //try again
//                                    } catch (Exception e) {
//                                        aContainerView.setVisibility(View.INVISIBLE);
//                                        e.printStackTrace();
//                                    }
//                                }
//                            }, 5000);
//                        } else {
//                            mAttempts = 0;
//                            //give up?
//                        }
//                    }
//                }
//            });
    }

    protected abstract void doLoadBanner(Activity aAct, View aBannerView, String aBannerID,AdBannerCallBack aCallback);


    protected abstract void doLoadNativeBanner(Activity aAct, ViewGroup aContainerView, String aNativeBannerID, AdBannerCallBack aCallback) throws Exception;
}
