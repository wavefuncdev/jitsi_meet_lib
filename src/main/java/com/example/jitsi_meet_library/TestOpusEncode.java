package com.example.jitsi_meet_library;

import android.util.Log;
import org.jitsi.impl.neomedia.codec.audio.opus.Opus;

@SuppressWarnings("unused")
public class TestOpusEncode {
    private static long opusEncoderAddress;
    private static final byte[] cacheOpusData = new byte[2048];

    @SuppressWarnings("unused")
    public static long Initialize()
    {
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
        Log.i("OpusEncode", "Encoder Address = " + opusEncoderAddress);
        return opusEncoderAddress;
    }

    @SuppressWarnings("unused")
    public static void Destroy()
    {
        if (opusEncoderAddress != 0) {
            Opus.encoder_destroy(opusEncoderAddress);
            opusEncoderAddress = 0;
        }
    }

    // オーディオデータ（PCM）のバイナリを受け取る
    // pcmData :
    @SuppressWarnings("unused")
    public static int SetAudioDataForPCM(byte[] pcmData) {
        Log.i("OpusEncode", "Pcm Size = " + pcmData.length);
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
        int opusDataSize;
        int result = Opus.encode(
            opusEncoderAddress,
            pcmData, 0, 128,
            cacheOpusData, 0, cacheOpusData.length);
        if (result == -1) {
            //失敗
            Log.e("OpusEncode", "failed encode");
            return -1;
        } else {
            opusDataSize = result;
        }

        Log.i("OpusEncode", "Opus Size = " + opusDataSize);
        return opusDataSize;
    }
}
