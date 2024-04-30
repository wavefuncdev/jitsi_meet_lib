package com.example.jitsi_meet_library;
import com.unity3d.player.UnityPlayer;

public class PluginTest {
    public static String FromUnity()
    {
        UnityPlayer.UnitySendMessage("UnityTest" , "FromAndroid" , "メソッド呼び出し");
        return( "戻り値" );
    }
}
