package com.aeriustech.utils.applovin;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.aeriustech.utils.BaseInterstitialManager;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxAdListener;
import com.applovin.mediation.MaxAdViewAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.ads.MaxAdView;
import com.applovin.mediation.ads.MaxInterstitialAd;
import com.applovin.sdk.AppLovinSdkUtils;

import java.util.NoSuchElementException;

public class AppLovinInterstitialManager extends BaseInterstitialManager {

    public AppLovinInterstitialManager(Context aAppContext, String aAdID, int aMinMSecsBetweenInters) {
        super(aAppContext, aAdID, aMinMSecsBetweenInters);
    }

    @Override
    protected void doLoadBanner(Activity aAct, View aBannerView, String aBannerID, AdBannerCallBack aCallback) {
        MaxAdView adView = (MaxAdView)aBannerView;
        // Get the adaptive banner height.
        int width = ViewGroup.LayoutParams.MATCH_PARENT;
        int heightDp = MaxAdFormat.BANNER.getAdaptiveSize( aAct ).getHeight();
        int heightPx = AppLovinSdkUtils.dpToPx( aAct, heightDp );
        ViewGroup.LayoutParams lparams= adView.getLayoutParams();
        lparams.height=heightPx;
        //adView.setLayoutParams( new FrameLayout.LayoutParams( width, heightPx ) );

        adView.setListener(new MaxAdViewAdListener() {
            @Override
            public void onAdExpanded(MaxAd ad) {

            }

            @Override
            public void onAdCollapsed(MaxAd ad) {

            }

            @Override
            public void onAdLoaded(MaxAd ad) {
                aCallback.onLoaded();

                AppLovinSdkUtils.Size adViewSize = adView.getAdFormat().getSize();
                int widthDp = adViewSize.getWidth();
                int heightDp = adViewSize.getHeight();
            }

            @Override
            public void onAdDisplayed(MaxAd ad) {

            }

            @Override
            public void onAdHidden(MaxAd ad) {

            }

            @Override
            public void onAdClicked(MaxAd ad) {

            }

            @Override
            public void onAdLoadFailed(String adUnitId, MaxError error) {
                aCallback.onFailed();
            }

            @Override
            public void onAdDisplayFailed(MaxAd ad, MaxError error) {
                aCallback.onFailed();
            }
        });

        // Set background or background color for banners to be fully functional
        //adView.setBackgroundColor( ... );
        // Load the ad
        adView.loadAd();
    }

    @Override
    protected void doLoadNativeBanner(Activity aAct, ViewGroup aContainerView, String aNativeBannerID, AdBannerCallBack aCallback) throws Exception {
        throw new NoSuchElementException("Native Banner not supported by applovin");
    }

    @Override
    protected boolean isInterstitialValid() {
        return interstitialAd!=null && interstitialAd.isReady();
    }

    @Override
    protected void doShowInterstitial(Activity aAct) {
        interstitialAd.showAd();
    }

    private MaxInterstitialAd interstitialAd;



    @Override
    protected void DoLoadInterstitialAd(Context aAppContext, AdInterCallBack aCallback) {
        if (interstitialAd==null)
            interstitialAd = new MaxInterstitialAd( mAdID, ((AppLovinApplication)aAppContext).currentActivity );

        interstitialAd.setListener(new MaxAdListener() {
            @Override
            public void onAdLoaded(MaxAd ad) {
                aCallback.onLoaded();
            }

            @Override
            public void onAdDisplayed(MaxAd ad) {
            }

            @Override
            public void onAdHidden(MaxAd ad) {
                aCallback.onAdDismissed();
            }

            @Override
            public void onAdClicked(MaxAd ad) {

            }

            @Override
            public void onAdLoadFailed(String adUnitId, MaxError error) {
                interstitialAd=null;
                aCallback.onFailed();
            }

            @Override
            public void onAdDisplayFailed(MaxAd ad, MaxError error) {
                aCallback.onAdFailedToShow();
            }
        });

        // Load the first ad
        interstitialAd.loadAd();
    }

}
