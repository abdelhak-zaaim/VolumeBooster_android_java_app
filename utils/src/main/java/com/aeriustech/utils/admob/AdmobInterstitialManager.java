package com.aeriustech.utils.admob;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowMetrics;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.aeriustech.utils.BaseInterstitialManager;
import com.aeriustech.utils.GDPRUtils;
import com.aeriustech.utils.R;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

import java.util.NoSuchElementException;

public class AdmobInterstitialManager extends BaseInterstitialManager {

    private static String TAG="com.AdmobManager";
    public AdmobInterstitialManager(Context aAppContext, String aAdID,  int aMinMSecsBetweenInters){
        super(aAppContext,  aAdID,  aMinMSecsBetweenInters);
    }

    //implementation
    private InterstitialAd mInterstitialAd=null;

    @Override
    protected boolean isInterstitialValid(){
        return mInterstitialAd!=null;
    }

    @Override
    protected void doShowInterstitial(Activity aAct){
        try{
            mInterstitialAd.show(aAct);
        }catch (Exception E){
            mInterstitialAd=null;
            E.printStackTrace();
            throw E;
        }
    }

    @Override
    protected void DoLoadInterstitialAd(Context aAppContext, AdInterCallBack aCallback) {
        Bundle extras = new Bundle();
        extras.putString("npa", "1"); // "1" for non-personalized ads
        AdRequest adRequest = null;
        if (new GDPRUtils().canShowPersonalizedAds(aAppContext)){
            Log.i(TAG,"CONSENTED");
            adRequest = new AdRequest.Builder()
                    .build();
        }else{
            Log.i(TAG,"NOT CONSENTED");
            adRequest = new AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter.class, extras)
                    .build();
        }

        InterstitialAd.load(aAppContext, mAdID, adRequest, new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        mInterstitialAd = null;
                        aCallback.onFailed();
                    }

                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        super.onAdLoaded(interstitialAd);
                        mInterstitialAd = interstitialAd;
                        mInterstitialAd.setFullScreenContentCallback(

                                new FullScreenContentCallback(){
                                    @Override
                                    public void onAdClicked() {
                                        // Called when a click is recorded for an ad.
                                    }

                                    @Override
                                    public void onAdDismissedFullScreenContent() {
                                        // Called when ad is dismissed.
                                        // Set the ad reference to null so you don't show the ad a second time.
                                        mInterstitialAd = null;
                                        aCallback.onAdDismissed();
                                    }

                                    @Override
                                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                                        // Called when ad fails to show.
                                        mInterstitialAd = null;
                                        aCallback.onAdFailedToShow();
                                    }

                                    @Override
                                    public void onAdImpression() {
                                        // Called when an impression is recorded for an ad.
                                    }

                                    @Override
                                    public void onAdShowedFullScreenContent() {
                                        // Called when ad is shown.
                                    }
                                }

                        );
                        aCallback.onLoaded();
                    }
                }
        );
    }

    // Determine the screen width (less decorations) to use for the ad width.
    public AdSize getAdSize(Activity aCtx, View aContainer ) {
        float adWidthPixels = aContainer.getWidth();
        // If the ad hasn't been laid out, default to the full screen width.
        float density;
        if (adWidthPixels == 0f) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                WindowMetrics windowMetrics = null;
                    windowMetrics = aCtx.getWindowManager().getCurrentWindowMetrics();
                Rect bounds = windowMetrics.getBounds();
                adWidthPixels = bounds.width();
                density = aCtx.getResources().getDisplayMetrics().density;
            }else{
                DisplayMetrics lmetrics=Resources.getSystem().getDisplayMetrics();
                adWidthPixels = lmetrics.widthPixels;
                density = lmetrics.density;
            }
        }else{
            density = aCtx.getResources().getDisplayMetrics().density;
        }

        int adWidth = (int) (adWidthPixels / density);
        AdSize lret=AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(aCtx, adWidth);
        return lret;
    }

    protected void doLoadBanner(Activity aAct, View aBannerView, String aBannerID,AdBannerCallBack aCallback){
        AdView adView = new AdView(aAct);
        ((ViewGroup)aBannerView).addView(adView);
        adView.setAdUnitId(aBannerID);
        try {
            AdSize lret = AdmobInterstitialManager.this.getAdSize(aAct, aBannerView);
            adView.setAdSize(lret);
        }catch (Exception E){
            E.printStackTrace();
        }

        Bundle extras = new Bundle();
        extras.putString("npa", "1"); // "1" for non-personalized ads
        AdRequest adRequest = null;
        if (new GDPRUtils().canShowPersonalizedAds(aAct.getApplicationContext())){
            Log.i(TAG,"CONSENTED");
            adRequest = new AdRequest.Builder()
                    .build();
        }else{
            Log.i(TAG,"NOT CONSENTED");
            adRequest = new AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter.class, extras)
                    .build();
        }


        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                aCallback.onLoaded();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                Log.i(TAG,"Banner failed to load: "+loadAdError);
                aCallback.onFailed();
            }
        });
        adView.loadAd(adRequest);
    }




    //---------   native banner ------------------

    protected void doLoadNativeBanner(Activity aAct, ViewGroup aContainerView, String aNativeBannerID, AdBannerCallBack aCallback){
//        try{
//            aContainerView.setVisibility(View.GONE);
//            AdLoader builder = (new AdLoader.Builder(aAct, aNativeBannerID)
//                    .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
//                        @Override
//                        public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
//                            displayNativeAd(aAct, aContainerView, nativeAd);
//                            aCallback.onLoaded();
//                        }
//                    })
//                    .withAdListener(new AdListener() {
//                        @Override
//                        public void onAdFailedToLoad(@NonNull LoadAdError adError) {
//                            aCallback.onFailed();
//                        }
//                }).build()); //https://developers.google.com/admob/android/native/options
//        }catch (Exception E){
//            aCallback.onFailed();
//        }
    }



    static public void displayNativeAd(Activity aAct, ViewGroup parent, NativeAd ad) {

        // Inflate a layout and add it to the parent ViewGroup.
        LayoutInflater inflater = (LayoutInflater) aAct
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        NativeAdView adView = (NativeAdView) inflater
                .inflate(R.layout.admob_native_ad, parent);

        // Locate the view that will hold the headline, set its text, and call the
        // NativeAdView's setHeadlineView method to register it.

        TextView headlineView = adView.findViewById(R.id.ad_headline);
        headlineView.setText(ad.getHeadline());
        adView.setHeadlineView(headlineView);

        // If the app is using a MediaView, it should be
        // instantiated and passed to setMediaView. This view is a little different
        // in that the asset is populated automatically, so there's one less step.
        MediaView mediaView = (MediaView) adView.findViewById(R.id.ad_media);
        adView.setMediaView(mediaView);

        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
        adView.setPriceView(adView.findViewById(R.id.ad_price));
        adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
        adView.setStoreView(adView.findViewById(R.id.ad_store));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));

        // Call the NativeAdView's setNativeAd method to register the
        // NativeAdObject.
        adView.setNativeAd(ad);

        // Ensure that the parent view doesn't already contain an ad view.
        parent.removeAllViews();

        // Place the AdView into the parent.
        parent.addView(adView);
    }
}