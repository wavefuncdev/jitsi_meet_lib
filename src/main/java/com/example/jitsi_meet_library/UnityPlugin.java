package com.example.jitsi_meet_library;

import com.facebook.react.modules.core.PermissionListener;
import com.unity3d.player.UnityPlayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.RestrictionEntry;
import android.content.RestrictionsManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

//import org.jitsi.meet.sdk.AudioModeModule;
import org.jitsi.meet.sdk.BroadcastEvent;
import org.jitsi.meet.sdk.ConnectionService;
import org.jitsi.meet.sdk.JitsiMeet;
import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetActivityDelegate;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.jitsi.meet.sdk.JitsiMeetOngoingConferenceService;
import org.jitsi.meet.sdk.JitsiMeetView;
import org.jitsi.meet.sdk.log.JitsiMeetLogger;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.Collection;
import java.util.HashMap;

import android.util.Base64;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

import org.jitsi.impl.neomedia.codec.audio.opus.Opus;

public class UnityPlugin {

    //private final static BroadcastReceiver broadcastReceiverTest = new BroadcastReceiver() {
        //@Override
        //public void onReceive(Context context, Intent intent) {
        //    onBroadcastReceivedTest(intent);
        //}
    //};

    private static String jitsiMeetServerUrl;
    private static String jitsiMeetRoomName;
    private static WebView webView;
    private static boolean isLoad;
    private static long opusEncoderAddress;
    private static byte[] cacheOpusData = new byte[2048];

    // url : 接続先サーバーのURL
    public static void Initialize()
    {
        Activity activity = UnityPlayer.currentActivity;

        // WebViewを取得
        webView = new WebView(activity);//activityで問題ない？
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                isLoad = true;
            }
        });

        // JavaScriptを有効にする
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        // lib-jitsi-meetのJavaScriptファイルをロード
        //webView.loadUrl("file:///android_asset/lib-jitsi-meet.min.js");
        // 外部サイトのJavaScriptを許可する
        //webSettings.setAllowUniversalAccessFromFileURLs(true);

        // 空のHTMLをロードする
        String emptyPage = "<html><body></body></html>";
        webView.loadData(emptyPage, "text/html", null);

        /*
        // Initialize default options for Jitsi Meet conferences.
        URL serverURL;
        try {
            // When using JaaS, replace "https://meet.jit.si" with the proper serverURL
            serverURL = new URL(url);
        } catch (MalformedURLException e) {
            //e.printStackTrace();
            throw new RuntimeException("Invalid server URL!");
        }

        // 接続情報のデフォルト値
        JitsiMeetConferenceOptions defaultOptions
            = new JitsiMeetConferenceOptions.Builder()
            .setServerURL(serverURL)
            // When using JaaS, set the obtained JWT here
            //.setToken("MyJWT")
            // Different features flags can be set
            // .setFeatureFlag("toolbox.enabled", false)
            // .setFeatureFlag("filmstrip.enabled", false)
            //.setFeatureFlag("welcomepage.enabled", false)
            .setAudioOnly(true)
            .build();
        JitsiMeet.setDefaultConferenceOptions(defaultOptions);

        registerForBroadcastMessages(activity);
        */
    }

    public static void Destroy()
    {
        Activity activity = UnityPlayer.currentActivity;
        //LocalBroadcastManager.getInstance(activity).unregisterReceiver(broadcastReceiverTest);

        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        if (opusEncoderAddress != 0) {
            Opus.encoder_destroy(opusEncoderAddress);
            opusEncoderAddress = 0;
        }
    }

    // 部屋に参加
    // roomName : 接続先の部屋名
    public static void Join(String url, String roomName)
    {
        //Activity activity = UnityPlayer.currentActivity;
        //JitsiMeetActivity.launch(activity, roomName);

        if (!isLoad)
        {
            throw new RuntimeException("WebView Now Loading!");
        }

        jitsiMeetServerUrl = url;
        jitsiMeetRoomName = roomName;

        // JavaScriptを使用してJitsi Meetの会議を作成する
        String javascript =
            "import JitsiMeetJS from 'lib-jitsi-meet';\n" +
            "\n" +
            "const options = {\n" +
            "  hosts: {\n" +
            "    domain: 'meet.jit.si',\n" +
            "    muc: 'conference.meet.jit.si' // Multi-User Chat room nickname\n" +
            "  },\n" +
            "  bosh: 'https://meet.jit.si/http-bind', // BOSH URL\n" +
            "  clientNode: 'http://jitsi.org/jitsimeet' // The name of client node advertised in XEP-0115 'c' stanza\n" +
            "};\n" +
            "\n" +
            "const connection = new JitsiMeetJS.JitsiConnection(null, null, options);\n" +
            "\n" +
            "connection.addEventListener(\n" +
            "  JitsiMeetJS.events.connection.CONNECTION_ESTABLISHED,\n" +
            "  () => {\n" +
            "    const conference = connection.initJitsiConference('" + roomName + "', {});\n" +
            "    conference.join();\n" +
            "  }\n" +
            ");\n" +
            "\n" +
            "connection.addEventListener(\n" +
            "  JitsiMeetJS.events.connection.CONNECTION_FAILED,\n" +
            "  () => {\n" +
            "    console.error('Connection Failed');\n" +
            "  }\n" +
            ");\n" +
            "\n" +
            "connection.connect();";

        // メインスレッドで送るためにpost
        webView.post(() -> webView.evaluateJavascript(javascript, null));
    }

    /*
    // Broadcastのリスナー設定
    private static void registerForBroadcastMessages(Context activity) {
        IntentFilter intentFilter = new IntentFilter();

        for (BroadcastEvent.Type type : BroadcastEvent.Type.values()) {
            intentFilter.addAction(type.getAction());
        }

        LocalBroadcastManager.getInstance(activity).registerReceiver(broadcastReceiverTest, intentFilter);
    }
    // Broadcastのリスナー
    private static void onBroadcastReceivedTest(Intent intent) {
        if (intent != null) {
            BroadcastEvent event = new BroadcastEvent(intent);

            switch (event.getType()) {
                case CONFERENCE_JOINED:
                    // サーバー接続完了
                    //Timber.i("Conference Joined with url%s", event.getData().get("url"));
                    break;
                case PARTICIPANT_JOINED:
                    // 他の人が接続完了
                    //Timber.i("Participant joined%s", event.getData().get("name"));
                    break;
            }
        }
    }
    //note:del = JitsiMeetActivityDelegate.getInstance();
    //note:del.addListenerの形がいいか？
     */

    // オーディオデータ（PCM）のバイナリを受け取る
    // pcmData :
    public static void SetAudioDataForPCM(byte[] pcmData)
    {
        passedAudioDataSize = pcmData.length;

        // TODO:Opus変換用のキャッシュに追加
        //変更点
        //PCMのキャッシュ化
        //(public)Flash関数を作り、Opusエンコード＆JavaScriptに送る処理を移動

        // Opusエンコード
        /*
         このメソッドは、Opusエンコーダーの構造体を作成し、それを指すポインターを返します。
         このポインターは long 型にキャストされます。
         ネイティブ関数の application パラメーターは常に OPUS_APPLICATION_VOIP に設定されます。
         Fs: 入力PCMのサンプルレート
         channels: 入力のチャンネル数（1または2）
         */
        opusEncoderAddress = Opus.encoder_create(
            44100, 1
        );
        /*
         PCMデータをOpusパケットにエンコードする
         encoder: 使用するエンコーダー。
         input: PCMエンコードされた入力を含む配列。
         inputOffset: input配列内のオフセット。
         inputFrameSize: input内の各チャンネルあたりのサンプル数。
         output: エンコードされたパケットが格納される配列。
         outputOffset: output配列内のオフセット。
         outputLength: outputで利用可能なバイト数。
         */
        int opusDataSize = 0;
        int result = Opus.encode(
            opusEncoderAddress,
            pcmData, 0, 1,
            cacheOpusData, 0, cacheOpusData.length);
        if (result == -1) {
            //失敗
            Log.e("OpusEncode", "failed encode");
            return;
        }
        else {
            opusDataSize = result;
        }
        byte[] opusData = java.util.Arrays.copyOfRange(cacheOpusData, 0, opusDataSize);

        SetBinaryToPassedJS(opusData);
        SendJitsiLocalTrack();
    }

    // JavaScriptに渡すバイナリを設定
    private static void SetBinaryToPassedJS(byte[] data)
    {
        base64Data = Base64.encodeToString(data, Base64.DEFAULT);
    }

    // JitsiMeetに送る
    private static void SendJitsiLocalTrack()
    {
        String javascript = "receiveAudioData('" + base64Data + "');\n" +
            "func receiveAudioData(base64Data) {\n" +
            "var opusData = atob(base64Data);\n" +
            "var blob = new Blob([new Uint8Array(opusData)], { type: 'audio/opus' });\n" +
            "var mediaStream = new MediaStream();\n" +
            "var audioTrack = mediaStream.addTrack(blob.stream());\n" +
            "JitsiMeetJS.createLocalTracksFromMediaStreams([{ stream: mediaStream, mediaType: 'audio' }])\n" +
            "    .then(tracks => {\n" +
            "        var opusJitsiLocalTrack = tracks[0];\n" +
            "        conference.addTrack(opusJitsiLocalTrack);\n" +
            "    })\n" +
            "    .catch(error => {\n" +
            "        console.error('Failed to create JitsiLocalTrack:', error);\n" +
            "    });";

        // メインスレッドで送るためにpost
        webView.post(() -> webView.evaluateJavascript(javascript, null));
    }

    // WebView上のJavaScriptに渡すBase64にしたデータ
    private static String base64Data;

    // 開発用データ
    private static int passedAudioDataSize;
    public static int GetPassedAudioDataSize() { return passedAudioDataSize; }




    public static String FromUnity(String str)
    {
        //TODO: UnityTestをGameObject名に変更する
        //TODO: FromAndroidを関数名に変更する
        //TODO: 引数を設定する
        UnityPlayer.UnitySendMessage("UnityTest" , "FromAndroid" , "");
        return( "UnityPlugin: " + str);
    }
    //note:インスタンス必要なはず、staticで保持

    //以下コピー（appのMainActivity.java）

    //**
    // * The request code identifying requests for the permission to draw on top
    // * of other apps. The value must be 16-bit and is arbitrarily chosen here.
    // */
    //private static final int OVERLAY_PERMISSION_REQUEST_CODE
    //    = (int) (Math.random() * Short.MAX_VALUE);
    //note:不要なのでコメント（Unity側で権限は制御するはず）

    /**
     * ServerURL configuration key for restriction configuration using {@link android.content.RestrictionsManager}
     */
    public static final String RESTRICTION_SERVER_URL = "https://webrtc-dev03.visualive.tokyo:8443";

    //**
    // * Broadcast receiver for restrictions handling
    // */
    //private BroadcastReceiver broadcastReceiver;
    //note:Viewの処理にしか使われていなかったのでコメント

    //**
    // * Flag if configuration is provided by RestrictionManager
    // */
    //private boolean configurationByRestrictions = false;
    //note:不要なのでコメント

    /**
     * Default URL as could be obtained from RestrictionManager
     */
    private String defaultURL;


    // JitsiMeetActivity overrides
    //

    //@Override
    //protected void onCreate(Bundle savedInstanceState) {
    //    JitsiMeet.showSplashScreen(this);
    //    super.onCreate(null);
    //}
    //note:スプラッシュ表示だけなのでコメント化

    //JitsiMeetActivityのonCreate()でinitialize()を呼び出すか判定する（falseを返すとinitialize()を呼び出す）
    //アプリのパーミッション設定を行う時はtrueを返し初期化の呼び出しをやめる
    //@Override
    //protected boolean extraInitialize() {
        //Log.d(this.getClass().getSimpleName(), "LIBRE_BUILD="+BuildConfig.LIBRE_BUILD);
        //note:ログなのでコメント

        // Setup Crashlytics and Firebase Dynamic Links
        // Here we are using reflection since it may have been disabled at compile time.
        //try {
            //Class<?> cls = Class.forName("org.jitsi.meet.GoogleServicesHelper");
            //Method m = cls.getMethod("initialize", JitsiMeetActivity.class);
            //m.invoke(null, this);
            //note:使わないと思うのでコメント
        //} catch (Exception e) {
            // Ignore any error, the module is not compiled when LIBRE_BUILD is enabled.
        //}

        // In Debug builds React needs permission to write over other apps in
        // order to display the warning and error overlays.
        //if (BuildConfig.DEBUG) {
        //    if (!Settings.canDrawOverlays(this)) {
        //        Intent intent
        //            = new Intent(
        //            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        //            Uri.parse("package:" + getPackageName()));

        //        startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);

        //        return true;
        //    }
        //}
        //note:デバッグ用コードなのでコメント

        //return false;
    //}
    //note:処理が無くなったのでコメント

    //onCreateで呼び出される
    //@Override
    //protected void initialize() {
    private void onInitialize() {
        final Activity activity = UnityPlayer.currentActivity;

        //broadcastReceiver = new BroadcastReceiver() {
            //@Override
            //public void onReceive(Context context, Intent intent) {
                // As new restrictions including server URL are received,
                // conference should be restarted with new configuration.
                //leave();//note:JitsiMeetActivity.javaの関数：View周りなのでコメント
                //recreate();//note:android.app.Activityの関数：アクティビティの作り直しはしないからコメント
            //}
        //};
        //note:overrideする構成にならないはずなのでコメント
        //note:権限の設定しなおしでアクティビティを作り直すための処理（の可能性）
        //note:権限の操作はUnity側で完結するため不要のはず

        //registerReceiver(broadcastReceiver,
        //    new IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED));
        //note:androidの関数:https://learn.microsoft.com/ja-jp/dotnet/api/android.content.context.registerreceiver?view=xamarin-android-sdk-13#android-content-context-registerreceiver(android-content-broadcastreceiver-android-content-intentfilter)
        //note:Receiverの追加はしないのでコメント

        //resolveRestrictions();//note:Restrictionsの確認（接続先URLの設定）
        //note:不要なのでコメント

        setJitsiMeetConferenceDefaultOptions();//note:サーバー接続情報のデフォルト値を設定

        //note:Viewの設定とデフォルト値のBundle（JitsiMeetConferenceOptions）をマージする
        //note:JitsiMeetView.javaのcreateReactRootViewを呼び出す
        //note:ReactRootViewの生成を行い、AndroidのViewに追加する
        //note:ReactRootViewはcom.facebook.react.ReactRootViewのためそれ以降の処理はわからない
        //super.initialize();
        //note:Viewの処理がメインのためコメント
        //note:必要な処理を抜き出す必要がある
        initialize();//note:JitsiMeetActivity.javaからコピーしてきたinitializeを呼び出し
    }

    // @Override
    //public void onDestroy() {
        //if (broadcastReceiver != null) {
        //    unregisterReceiver(broadcastReceiver);
        //    broadcastReceiver = null;
        //}
        //note:不要なのでコメント

        //note:Audioの切断
        //note:Videoの切断
        //note:activity.unregisterReceiver
        //note:ReactのDestroy（com.facebook.react）
        //super.onDestroy();
        //note:Audioとactivity.unregisterReceiverを抜き出す
    //}
    //note:JitsiMeetActivityからコピーしたonDestroy()側を使うのでコメント

    //note:initializeから呼び出す
    //note:サーバー接続情報のデフォルト値を設定
    private void setJitsiMeetConferenceDefaultOptions() {
        // Set default options
        JitsiMeetConferenceOptions defaultOptions
            = new JitsiMeetConferenceOptions.Builder()//note:Bundleインスタンス生成
            .setServerURL(buildURL(defaultURL))
            .setFeatureFlag("welcomepage.enabled", true)//note:不要な情報と思うがdefaultはfalseなのか調査
            //.setFeatureFlag("resolution", 360)//note:不要な情報と思うのでコメント
            //.setFeatureFlag("server-url-change.enabled", !configurationByRestrictions)//note:不要な情報と思うのでコメント
            .build();
        //note:setServerURLは接続先URLを設定
        //note:setFeatureFlagはサーバー側で使う情報？
        //note:buildはインスタンス生成（コピー）

        JitsiMeet.setDefaultConferenceOptions(defaultOptions);
        //note:デフォ値の設定
    }

    //note:initializeから呼び出す
    //note:Restrictionsの確認（接続先URLの設定）
    //private void resolveRestrictions() {
        //note:制限の取得
    //    RestrictionsManager manager =
    //        (RestrictionsManager) getSystemService(Context.RESTRICTIONS_SERVICE);
    //    Bundle restrictions = manager.getApplicationRestrictions();
    //    Collection<RestrictionEntry> entries = manager.getManifestRestrictions(
    //        getApplicationContext().getPackageName());

        //note:接続先URLの設定
    //    for (RestrictionEntry restrictionEntry : entries) {
    //        String key = restrictionEntry.getKey();
    //        if (RESTRICTION_SERVER_URL.equals(key)) {
                // If restrictions are passed to the application.
    //            if (restrictions != null &&
    //                restrictions.containsKey(RESTRICTION_SERVER_URL)) {
    //                defaultURL = restrictions.getString(RESTRICTION_SERVER_URL);
    //                configurationByRestrictions = true;
                    // Otherwise use default URL from app-restrictions.xml.
    //            } else {
    //                defaultURL = restrictionEntry.getSelectedString();
    //                configurationByRestrictions = false;
    //            }
    //        }
    //    }
    //}
    //note:「Restriction（制限）」は考慮する必要がないのでコメント（Unity側で制御されるはず）
    //note:接続先のURLは抜き出す

    // Activity lifecycle method overrides
    //

    //@Override
    //public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
        //    if (Settings.canDrawOverlays(this)) {
        //        initialize();
        //        return;
        //    }
        //    throw new RuntimeException("Overlay permission is required when running in Debug mode.");
        //}
        //note:権限の操作はUnity側で行うのでコメント

        //super.onActivityResult(requestCode, resultCode, data);
        //note:reactのDestroy（com.facebook.react）
        //note:react以外の処理がないのでコメント
    //}
    //note:処理がないのでコメント

    // ReactAndroid/src/main/java/com/facebook/react/ReactActivity.java
    //@Override
    //public boolean onKeyUp(int keyCode, KeyEvent event) {
        //if (BuildConfig.DEBUG && keyCode == KeyEvent.KEYCODE_MENU) {
            //JitsiMeet.showDevOptions();//note:reactの処理しかないのでコメント（com.facebook.react）
            //return true;
        //}
        //note:処理がないのでコメント

        //return super.onKeyUp(keyCode, event);
    //}
    //note:処理がないのでコメント

    //@Override
    //public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
    //    super.onPictureInPictureModeChanged(isInPictureInPictureMode);

    //    Log.d(TAG, "Is in picture-in-picture mode: " + isInPictureInPictureMode);

    //    if (!isInPictureInPictureMode) {
    //        this.startActivity(new Intent(this, getClass())
    //            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
    //    }
    //}
    //note:「バッググランドに移行した時に小さい画面でアプリ画面を表示する機能」は不要なのでコメント

    // Helper methods
    //

    private @Nullable URL buildURL(String urlStr) {
        try {
            return new URL(urlStr);
        } catch (Exception e) {
            return null;
        }
    }


    //以下コピー（sdkのJitsiMeetActivity.java）

    protected static final String TAG = JitsiMeetActivity.class.getSimpleName();

    private static final String ACTION_JITSI_MEET_CONFERENCE = "org.jitsi.meet.CONFERENCE";
    private static final String JITSI_MEET_CONFERENCE_OPTIONS = "JitsiMeetConferenceOptions";

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onBroadcastReceived(intent);
        }
    };

    /**
     * Instance of the {@link JitsiMeetView} which this activity will display.
     */
    private JitsiMeetView jitsiView;

    // Helpers for starting the activity
    //

    public static void launch(Context context, JitsiMeetConferenceOptions options) {
        Intent intent = new Intent(context, JitsiMeetActivity.class);
        intent.setAction(ACTION_JITSI_MEET_CONFERENCE);
        intent.putExtra(JITSI_MEET_CONFERENCE_OPTIONS, options);
        if (!(context instanceof Activity)) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    public static void launch(Context context, String url) {
        JitsiMeetConferenceOptions options
            = new JitsiMeetConferenceOptions.Builder().setRoom(url).build();
        launch(context, options);
    }

    // Overrides
    //

    //@Override
    //public void onConfigurationChanged(Configuration newConfig) {
    //    super.onConfigurationChanged(newConfig);
    //    Intent intent = new Intent("onConfigurationChanged");
    //    intent.putExtra("newConfig", newConfig);
    //    this.sendBroadcast(intent);
    //}

    //@Override
    protected void onCreate(Bundle savedInstanceState) {
        //super.onCreate(savedInstanceState);
        //note:Activityは使わないで実現したいのでコメント

        //setContentView(org.jitsi.meet.sdk.R.layout.activity_jitsi_meet);
        //this.jitsiView = findViewById(org.jitsi.meet.sdk.R.id.jitsiView);
        //note:表示機能は不要なのでコメント

        registerForBroadcastMessages();//note:Broadcastの設定

        //if (!extraInitialize()) {//note:追加の初期化（権限などの操作）は不要なのでコメント
            //initialize();
            onInitialize();
        //}
    }

    //@Override
    public void onResume() {
        //super.onResume();
        //note:Activityは使わないで実現したいのでコメント

        //JitsiMeetActivityDelegate.onHostResume(this);
        JitsiMeetActivityDelegate.onHostResume(UnityPlayer.currentActivity);
    }

    //@Override
    public void onStop() {
        //JitsiMeetActivityDelegate.onHostPause(this);
        JitsiMeetActivityDelegate.onHostPause(UnityPlayer.currentActivity);

        //super.onStop();
        //note:Activityは使わないで実現したいのでコメント
    }

    //@Override
    public void onDestroy() {
        // Here we are trying to handle the following corner case: an application using the SDK
        // is using this Activity for displaying meetings, but there is another "main" Activity
        // with other content. If this Activity is "swiped out" from the recent list we will get
        // Activity#onDestroy() called without warning. At this point we can try to leave the
        // current meeting, but when our view is detached from React the JS <-> Native bridge won't
        // be operational so the external API won't be able to notify the native side that the
        // conference terminated. Thus, try our best to clean up.
        //leave();
        //note:Viewの処理しかないのでコメント

        //this.jitsiView = null;
        //note:Viewは使わないのでコメント

        //if (AudioModeModule.useConnectionService()) {//note:外に公開されていないのでjitsiMeetActivity.javaを経由して呼び出す
        //    ConnectionService.abortConnections();//note:外に公開されていないのでjitsiMeetActivity.javaを経由して呼び出す
        //}
        //note:SDK内の修正になるので、いったんコメント

        Activity activity = UnityPlayer.currentActivity;
        //JitsiMeetOngoingConferenceService.abort(this);
        JitsiMeetOngoingConferenceService.abort(activity);

        //LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(broadcastReceiver);

        //JitsiMeetActivityDelegate.onHostDestroy(this);
        JitsiMeetActivityDelegate.onHostDestroy(activity);

        //super.onDestroy();
        //note:Activityは使わないで実現したいのでコメント
    }

    //@Override
    //public void finish() {
        //leave();
        //note:Viewの処理しかないのでコメント

        //super.finish();
        //note:Activityは使わないで実現したいのでコメント
    //}
    //note:処理がないのでコメント

    // Helper methods
    //

    //protected JitsiMeetView getJitsiView() {
    //    return jitsiView;
    //}
    //note:Viewは使わないのでコメント

    public void join(@Nullable String url) {
        JitsiMeetConferenceOptions options
            = new JitsiMeetConferenceOptions.Builder()
            .setRoom(url)
            .build();
        join(options);
    }

    public void join(JitsiMeetConferenceOptions options) {
        if (this.jitsiView  != null) {
            this.jitsiView .join(options);
        } else {
            JitsiMeetLogger.w("Cannot join, view is null");
        }
    }

    //protected void leave() {
    //    if (this.jitsiView != null) {
    //        this.jitsiView.abort();
    //    } else {
    //        JitsiMeetLogger.w("Cannot leave, view is null");
    //    }
    //}
    //note:Viewの処理はいらないのでコメント

    //private @Nullable
    //JitsiMeetConferenceOptions getConferenceOptions(Intent intent) {
    //    String action = intent.getAction();

    //    if (Intent.ACTION_VIEW.equals(action)) {
    //        Uri uri = intent.getData();
    //        if (uri != null) {
    //            return new JitsiMeetConferenceOptions.Builder().setRoom(uri.toString()).build();
    //        }
    //    } else if (ACTION_JITSI_MEET_CONFERENCE.equals(action)) {
    //        return intent.getParcelableExtra(JITSI_MEET_CONFERENCE_OPTIONS);
    //    }

    //    return null;
    //}
    //note:わざわざintentを操作する必要はなさそうなのでコメント
    //note:接続情報を返せればいい

    //**
    // * Helper function called during activity initialization. If {@code true} is returned, the
    // * initialization is delayed and the {@link JitsiMeetActivity#initialize()} method is not
    // * called. In this case, it's up to the subclass to call the initialize method when ready.
    // * <p>
    // * This is mainly required so we do some extra initialization in the Jitsi Meet app.
    // *
    // * @return {@code true} if the initialization will be delayed, {@code false} otherwise.
    // */
    //protected boolean extraInitialize() {
    //    return false;
    //}
    //note:不要なのでコメント

    //protected void initialize() {
    public void initialize() {//note:Unity側から呼ぶことになると思うので公開
        // Join the room specified by the URL the app was launched with.
        // Joining without the room option displays the welcome page.
        //join(getConferenceOptions(getIntent()));//note:接続情報を取得できればいい
        //note:下記に書き直し

        //note:接続情報を作成
        String url = "";
        JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder().setRoom(url).build();
        join(options);
        //note:「public void join(@Nullable String url)」と処理がかぶってる
    }
    //note:インスタンスを使いまわせて部屋名だけ変えて使えるなら、関数の処理を整理する
    //note:インスタンス生成と部屋名指定と接続を分ける

    protected void onConferenceJoined(HashMap<String, Object> extraData) {
        JitsiMeetLogger.i("Conference joined: " + extraData);
        // Launch the service for the ongoing notification.

        //JitsiMeetOngoingConferenceService.launch(this, extraData);
        JitsiMeetOngoingConferenceService.launch(UnityPlayer.currentActivity, extraData);
    }

    protected void onConferenceTerminated(HashMap<String, Object> extraData) {
        JitsiMeetLogger.i("Conference terminated: " + extraData);
    }

    protected void onConferenceWillJoin(HashMap<String, Object> extraData) {
        JitsiMeetLogger.i("Conference will join: " + extraData);
    }

    protected void onParticipantJoined(HashMap<String, Object> extraData) {
        try {
            JitsiMeetLogger.i("Participant joined: ", extraData);
        } catch (Exception e) {
            JitsiMeetLogger.w("Invalid participant joined extraData", e);
        }
    }

    protected void onParticipantLeft(HashMap<String, Object> extraData) {
        try {
            JitsiMeetLogger.i("Participant left: ", extraData);
        } catch (Exception e) {
            JitsiMeetLogger.w("Invalid participant left extraData", e);
        }
    }

    protected void onReadyToClose() {
        JitsiMeetLogger.i("SDK is ready to close");
        //finish();
        //note:処理がないのでコメント
    }

//    protected void onTranscriptionChunkReceived(HashMap<String, Object> extraData) {
//        JitsiMeetLogger.i("Transcription chunk received: " + extraData);
//    }

//    protected void onCustomOverflowMenuButtonPressed(HashMap<String, Object> extraData) {
//        JitsiMeetLogger.i("Custom overflow menu button pressed: " + extraData);
//    }

    // Activity lifecycle methods
    //

    //@Override
    //public void onActivityResult(int requestCode, int resultCode, Intent data) {
    //    super.onActivityResult(requestCode, resultCode, data);
//
    //    JitsiMeetActivityDelegate.onActivityResult(this, requestCode, resultCode, data);
    //}
    //note:アクティビティの切り替えはしないはずなのでコメント

    //@Override
    //note:アクティビティの切り替えはしないはずなのでコメント
    public void onBackPressed() {
        JitsiMeetActivityDelegate.onBackPressed();
    }

    //@Override
    //public void onNewIntent(Intent intent) {
    //    super.onNewIntent(intent);

    //    JitsiMeetConferenceOptions options;

    //    if ((options = getConferenceOptions(intent)) != null) {//note:接続情報を取得できればいい
    //        join(options);
    //        return;
    //    }

    //    JitsiMeetActivityDelegate.onNewIntent(intent);
    //}
    //note:Activityの作り直されたときに呼ばれる処理は不要なはず。コメント

    //@Override
    //protected void onUserLeaveHint() {
    //    if (this.jitsiView  != null) {
    //        this.jitsiView .enterPictureInPicture();
    //    }
    //}
    //note:HOMEボタンが押された時の処理はUnity側で制御するはずなのでコメント

    // JitsiMeetActivityInterface
    //

    //@Override
    //public void requestPermissions(String[] permissions, int requestCode, PermissionListener listener) {
    //    JitsiMeetActivityDelegate.requestPermissions(this, permissions, requestCode, listener);
    //}
    //note:権限の制御はUnity側で制御するのでコメント

    //@SuppressLint("MissingSuperCall")
    //@Override
    //public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    //    JitsiMeetActivityDelegate.onRequestPermissionsResult(requestCode, permissions, grantResults);
    //}
    //note:権限の制御はUnity側で制御するのでコメント

    private void registerForBroadcastMessages() {
        IntentFilter intentFilter = new IntentFilter();

        for (BroadcastEvent.Type type : BroadcastEvent.Type.values()) {
            intentFilter.addAction(type.getAction());
        }

        //LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
        LocalBroadcastManager.getInstance(UnityPlayer.currentActivity).registerReceiver(broadcastReceiver, intentFilter);
    }

    private void onBroadcastReceived(Intent intent) {
        if (intent != null) {
            BroadcastEvent event = new BroadcastEvent(intent);

            switch (event.getType()) {
                case CONFERENCE_JOINED:
                    onConferenceJoined(event.getData());
                    break;
                case CONFERENCE_WILL_JOIN:
                    onConferenceWillJoin(event.getData());
                    break;
                case CONFERENCE_TERMINATED:
                    onConferenceTerminated(event.getData());
                    break;
                case PARTICIPANT_JOINED:
                    onParticipantJoined(event.getData());
                    break;
                case PARTICIPANT_LEFT:
                    onParticipantLeft(event.getData());
                    break;
                case READY_TO_CLOSE:
                    onReadyToClose();
                    break;
//                case TRANSCRIPTION_CHUNK_RECEIVED:
//                    onTranscriptionChunkReceived(event.getData());
//                    break;
//                case CUSTOM_OVERFLOW_MENU_BUTTON_PRESSED:
//                    onCustomOverflowMenuButtonPressed(event.getData());
//                    break;
            }
        }
    }
}