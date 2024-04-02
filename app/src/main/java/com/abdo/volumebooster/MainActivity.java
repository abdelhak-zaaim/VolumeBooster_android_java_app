package com.abdo.volumebooster;

import static android.media.audiofx.AudioEffect.EFFECT_TYPE_EQUALIZER;
import static android.media.audiofx.AudioEffect.EFFECT_TYPE_LOUDNESS_ENHANCER;
import static android.media.audiofx.AudioEffect.queryEffects;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aeriustech.utils.BaseAdApplication;
import com.aeriustech.utils.CircularProgressBar;
import com.abdo.volumebooster.util.Constant;
import com.google.android.material.navigation.NavigationView;


import it.beppi.knoblibrary.Knob;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener  {

    ImageButton play,next,previous;
    ImageView settings;
    AudioManager audioManager;
    Button btn_60_percent,btn_100_percent,btn_160_percent,btn_max;
    Knob knob;
    public int maxVolume;
    CircularProgressBar progressBar;
    TextView track;
    BroadcastReceiver trackChanged;
    public DrawerLayout drawerLayout;
    public  NavigationView navigationView;

    private int targetGain = 0;
    private Equalizer equalizer;
    private LoudnessEnhancer loudnessEnhancer=null;
    private boolean isFirstPressed=true;
    public static LinearLayout loading;
    public static String TAG="com.aadevelopers.MainActivity";
    private boolean initialLayoutComplete=false;
    private boolean mLoudEnhancerSupported=false;
    private boolean mEquSupported=false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.main_2 );
        initUI();
      // setLoudEnable( true );

        AudioEffect.Descriptor[] lfxs=queryEffects();
        for (AudioEffect.Descriptor lfx:lfxs) {
            if (lfx.type.toString().equals(EFFECT_TYPE_EQUALIZER.toString())){
                mEquSupported=true;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    //equalizer = new Equalizer(Integer.MAX_VALUE, new MediaPlayer().getAudioSessionId());
                    try {
                        equalizer = new Equalizer(Integer.MAX_VALUE, 0);
                        equalizer.setEnabled(true);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
            if (lfx.type.toString().equals(EFFECT_TYPE_LOUDNESS_ENHANCER.toString())){
                try {
                    mLoudEnhancerSupported=true;
                    loudnessEnhancer = new LoudnessEnhancer(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }



        // VolumeBoosterService.init(this);
        View mAdView = findViewById(R.id.adView);
        mAdView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (!initialLayoutComplete) {
                            initialLayoutComplete = true;
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((BaseAdApplication)MainActivity.this.getApplication()).loadBanner(MainActivity.this,mAdView, getString(R.string.banner_id) );
                                }
                            });
                        }
                    }
                });

        trackChanged = new BroadcastReceiver() {
        public void onReceive(Context context, final Intent intent) {
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        String string = intent.getExtras().getString("artist");
                        String _track=intent.getExtras().getString("track");
                        track.setText( _track  + " a "+string );
                    } catch (Exception unused2) {
                    }
                }
            });
            }
        };
        audioManager = (AudioManager) getSystemService( Context.AUDIO_SERVICE );
        knob.setOnTouchUpListener(new Knob.TouchUpHandler() {
                                      @Override
                                      public void onUp() {
                                          interstitialCheck();
                                      }
                                  });

        knob.setOnStateChanged(new Knob.OnStateChanged() {
            @Override
            public void onState(int state) {

                Vibrator vibrator = (Vibrator) MainActivity.this.getSystemService(Context.VIBRATOR_SERVICE);
                if (Build.VERSION.SDK_INT >= 26) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, -1));
                } else {
                    vibrator.vibrate(100);
                }
                float f = (float) state;
                Log.e("knowStad", " " + state);
                targetGain = state;
                // MainActivity.this.progressBar.setProgress(f);
                MainActivity.this.btnReset();
                if (state >= 269) {
                    setLoudEnable(true);
                    setSystemVolume( 15 );
                    setTargetGain(10000);
                    //setMaxVolume();
                    MainActivity.this.btn_max.setBackground(getDrawable(R.drawable.btn_press));
                } else if (state >= 162) {
                    setLoudEnable(true);
                    setSystemVolume( 13 );
                    setTargetGain(5000);
                    MainActivity.this.btn_160_percent.setBackground(getDrawable(R.drawable.btn_press));
                } else if (state >= 100) {
                    setLoudEnable(true);
                    setSystemVolume(10);
                    setTargetGain(3000);
                    //  setSystemVolume( 10 );
                    MainActivity.this.btn_100_percent.setBackground(getDrawable(R.drawable.btn_press));
                } else if (state <= 60) {
                    setLoudEnable(true);
                    setSystemVolume(5);
                    setTargetGain(1000);
                    //    setSystemVolume( 5 );
                    MainActivity.this.btn_60_percent.setBackground(getDrawable(R.drawable.btn_press));

                    try {
                        testNative();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }


        /*Intent intent = new Intent(Constant.CHANGE_BOOST);
        intent.putExtra("boostPercent", (f / 2.69f) / 100.0f);
        MainActivity.this.sendBroadcast(intent);*/
                //  AppPreferences.getInstance(MainActivity.this).saveData("knob_value", i);

                //    setLoudEnable(true);
                MainActivity.this.progressBar.setProgress(f);


            }
        });
        play.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (MainActivity.this.checkActiveMusic()) {
                    KeyEvent keyEvent = new KeyEvent(0, 127);
                    if (Build.VERSION.SDK_INT >= 19) {
                        audioManager.dispatchMediaKeyEvent(keyEvent);
                    }
                } else {
                    KeyEvent keyEvent2 = new KeyEvent(0, 126);
                    if (Build.VERSION.SDK_INT >= 19) {
                        audioManager.dispatchMediaKeyEvent(keyEvent2);
                    }
                }
                MainActivity.this.checkMusicState();
               /* new Handler( Looper.getMainLooper()).postDelayed( new Runnable() {
                    public void run() {

                    }
                }, 50);*/
            }
        } );
        this.maxVolume = audioManager.getStreamMaxVolume(3);
        Log.e( "maaxx",""+maxVolume );
        int streamVolume = audioManager.getStreamVolume(3);

        btn_60_percent.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnReset();
                btn_60_percent.setBackground( getDrawable( R.drawable.btn_press ) );
               // MainActivity.this.btn_60_percent.setBackground(ThemesUtils.getBtnSelectPercents(MainActivity.this));
                MainActivity.this.knob.forceState(0, false);
                interstitialCheck();
               // MainActivity.this.progressBar.setProgress(0.0f);
                MainActivity mainActivity = MainActivity.this;
              //  mainActivity.setSystemVolume((int) (((float) mainActivity.maxVolume) * 0.6f));
                mainActivity.setSystemVolume(5);
               // MainActivity.this.seekBar.setProgress((int) (((float) MainActivity.this.maxVolume) * 0.6f));
                setLoudEnable( true );
                setTargetGain(1000);


               // MainActivity.this.btn_160_percent.setBackground(ThemesUtils.getBtnSelectPercents(MainActivity.this));
              //  MainActivity.this.knob_volume.setStateNoListener(162, false);
              //  MainActivity.this.progressBar.setProgress(162.0f);

            }
        } );
        btn_100_percent.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnReset();
                btn_100_percent.setBackground( getDrawable( R.drawable.btn_press ) );
                knob.forceState( 100 );
                interstitialCheck();
                MainActivity.this.progressBar.setProgress(0.0f);
                MainActivity mainActivity = MainActivity.this;
                mainActivity.setSystemVolume(10);
                setLoudEnable( true );
                setTargetGain(3000);
              /*  Intent intent = new Intent(Constant.CHANGE_BOOST);
                intent.putExtra("boostPercent", 0.0f);
                MainActivity.this.sendBroadcast(intent);*/
            }
        } );
        btn_160_percent.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                btnReset();
                btn_160_percent.setBackground( getDrawable( R.drawable.btn_press ) );
                //
                interstitialCheck();
                MainActivity.this.knob.forceState(162, true);
                MainActivity mainActivity = MainActivity.this;
                mainActivity.setSystemVolume(13);
                setLoudEnable( true );
                setTargetGain(5000);
               // MainActivity.this.progressBar.setProgress(162.0f);
              /*  Intent intent = new Intent(Constant.CHANGE_BOOST);
                intent.putExtra("boostPercent", 0.6f);
                MainActivity.this.sendBroadcast(intent);*/


            }
        } );
        btn_max.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                btnReset();
                btn_max.setBackground( getDrawable( R.drawable.btn_press ) );
                interstitialCheck();
                MainActivity.this.setMaxVolume();
                setLoudEnable( true );
                setTargetGain(10000);

            }
        } );




        settings.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.openDrawer((int) GravityCompat.START);
            }
        } );



        next.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                KeyEvent keyEvent = new KeyEvent(0, 87);
                if (Build.VERSION.SDK_INT >= 19) {
                    audioManager.dispatchMediaKeyEvent(keyEvent);
                }
            }
        } );

        previous.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                KeyEvent keyEvent = new KeyEvent(0, 87);
                if (Build.VERSION.SDK_INT >= 19) {
                    audioManager.dispatchMediaKeyEvent(keyEvent);
                }
            }
        } );


        
        IntentFilter iF = new IntentFilter();
        iF.addAction("com.android.music.metachanged");

        registerReceiver(mReceiver, iF);

    }




    boolean firstTime=true;
    int counter=BuildConfig.clicksToFirstInter;



    private void maxMusic() {
        Intent intent = new Intent(Constant.CHANGE_BOOST);
        intent.putExtra("boostPercent", 1.0f);
        sendBroadcast(intent);
    }

    private void mediumMusic() {
        MainActivity mainActivity = MainActivity.this;
        mainActivity.setSystemVolume(mainActivity.maxVolume);
      /*  Intent intent = new Intent(Constant.CHANGE_BOOST);
        intent.putExtra("boostPercent", 0.0f);
        MainActivity.this.sendBroadcast(intent);*/
    }

    private void slowMusic() {
        MainActivity mainActivity = MainActivity.this;
        mainActivity.setSystemVolume((int) (((float) mainActivity.maxVolume) * 0.6f));
        /*Intent intent = new Intent(Constant.CHANGE_BOOST);
        intent.putExtra("boostPercent", 0.0f);
        MainActivity.this.sendBroadcast(intent);*/
    }

    private void interstitialCheck(){
        counter++;
        Log.i(TAG,"Interstitial:"+counter);
        if (counter>=BuildConfig.interClicksInterval){
            ((BaseAdApplication) getApplication()).showInterstitial(this, new Runnable() {
                @Override
                public void run() {
                    counter=0;
                }
            });
        }else{
            Log.i(TAG,"Cannot show ads");

        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("command");
            Log.v("tag ", action + " / " + cmd);
            String artist = intent.getStringExtra("artist");
            String album = intent.getStringExtra("album");
            String track = intent.getStringExtra("track");
            Log.v("tag", artist + ":" + album + ":" + track);
            MainActivity.this.track.setText( track );
          //  Toast.makeText(MainActivity.this, track, Toast.LENGTH_SHORT).show();
        }
    };

    public void setSystemVolume(int i) {
        ((AudioManager) getSystemService(Context.AUDIO_SERVICE)).setStreamVolume(3, i, 0);
    }

    public void setMaxVolume(){
       // audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 105, 0);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
        this.knob.forceState(269, true);
        Intent intent = new Intent(Constant.CHANGE_BOOST);
        intent.putExtra("boostPercent", 2.0f);
        sendBroadcast(intent);
    }

    private void btnReset() {
        btn_60_percent.setBackground( getDrawable( R.drawable.btn_per ) );
        btn_100_percent.setBackground( getDrawable( R.drawable.btn_per ) );
        btn_160_percent.setBackground( getDrawable( R.drawable.btn_per ) );
        btn_max.setBackground( getDrawable( R.drawable.btn_per ) );
    }

    public void checkMusicState() {
        if (checkActiveMusic()) {
            play.setImageResource(R.drawable.ic_play);
        }else{
            play.setImageResource(R.drawable.ic_pause);
        }
      /*  ImageButton imageButton2 = this.playPause;
        Resources resources2 = getResources();
        imageButton2.setImageResource(resources2.getIdentifier("btn_play_0" + (this.theme + 1), "drawable", getPackageName()));*/
       /* if (this.currentArtist != null) {
            this.trackTitle.setText("Open Music Player");
            this.trackArtist.setVisibility(8);
        }*/
    }

    private void initUI() {
        loading=findViewById( R.id.linearLayout );
        progressBar=findViewById( R.id.progressBar );
        knob=findViewById( R.id.knob_volume );
        play=findViewById( R.id.btn_play );
        next=findViewById( R.id.btn_next );
        previous=findViewById( R.id.btn_previous );
        btn_60_percent=findViewById( R.id.btn_60_percent );
        btn_100_percent=findViewById( R.id.btn_100_percent );
        btn_160_percent=findViewById( R.id.btn_160_percent );
        btn_max=findViewById( R.id.btn_max );
        track=findViewById( R.id.textView );
        settings=findViewById( R.id.btn_settings );
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationView=findViewById( R.id.nav_view );
        navigationView.setNavigationItemSelectedListener(this);
    }

    public boolean checkActiveMusic() {
        return ((AudioManager) getSystemService(Context.AUDIO_SERVICE )).isMusicActive();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.rate:
                final String appPackageName = getPackageName();
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity( new Intent( Intent.ACTION_VIEW, Uri.parse( "https://play.google.com/store/apps/details?id=" + appPackageName ) ) );
                }
                break;
            case R.id.share:
                try {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Fast Diet");
                    String shareMessage= "\nCheck out this Great Application\n\n";
                    shareMessage = shareMessage + "https://play.google.com/store/apps/details?id=" + getPackageName() +"\n\n";
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                    startActivity(Intent.createChooser(shareIntent, "Select any"));
                } catch(Exception e) {
                    //e.toString();
                }
                break;

            case R.id.contact:
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:tkhumush@oulook.com"));
                emailIntent.putExtra(Intent.EXTRA_EMAIL, "");
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Your subject");
                emailIntent.putExtra(Intent.EXTRA_TEXT, "Email message goes here");

                try {
                    startActivity( Intent.createChooser( emailIntent, "Send mail..." ) );
                }catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(MainActivity.this, "There is no email client installed.", Toast.LENGTH_SHORT).show();
                }
                break;


        }
        return true;
    }


    private void setTargetGain(int value) {
        if (loudnessEnhancer != null) {
            loudnessEnhancer.setTargetGain(value);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private boolean setLoudEnable(boolean isEnable) {
        if (loudnessEnhancer != null) {
            try {
                loudnessEnhancer.setEnabled(isEnable);
                loudnessEnhancer.release();
            }catch (Exception E){
                E.printStackTrace();
            }
            loudnessEnhancer = null;
        }
        if (isEnable && mLoudEnhancerSupported) {
            loudnessEnhancer = new LoudnessEnhancer(0);
            loudnessEnhancer.setEnabled(true);
            return setLoudBase(true);
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private boolean setLoudBase(boolean isEnable) {
        if (loudnessEnhancer != null) {
            try {
                loudnessEnhancer.setEnabled(isEnable);
                loudnessEnhancer.release();
            }catch (Exception E){
                E.printStackTrace();
            }
            loudnessEnhancer = null;
        }
        if (isEnable && mLoudEnhancerSupported) {
            loudnessEnhancer = new LoudnessEnhancer(0);
            loudnessEnhancer.setEnabled(true);

            loudnessEnhancer.setEnabled(false);
            loudnessEnhancer.release();
            loudnessEnhancer = null;

            loudnessEnhancer = new LoudnessEnhancer(0);
            loudnessEnhancer.setEnabled(true);

            loudnessEnhancer.setTargetGain(targetGain);
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loudnessEnhancer !=null) {
            loudnessEnhancer.setEnabled(false);
            loudnessEnhancer.release();
            loudnessEnhancer = null;
        }
        if (equalizer !=null ) {
            equalizer.setEnabled(false);
            equalizer.release();
            equalizer = null;
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        /*
        if(isAdAvailable()){
                FullScreenContentCallback fullScreenContentCallback =new FullScreenContentCallback() {

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        super.onAdDismissedFullScreenContent();

                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        super.onAdFailedToShowFullScreenContent(adError);
                    }
                };
                appOpenAd.setFullScreenContentCallback(fullScreenContentCallback);
                appOpenAd.show(MainActivity.this);

        } */
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    private void testNative() throws Exception {
       // ((BaseAdApplication)getApplication()).loadNativeBanner(this, findViewById(R.id.native_view), getString(R.string.native_banner_id));
    }

}
