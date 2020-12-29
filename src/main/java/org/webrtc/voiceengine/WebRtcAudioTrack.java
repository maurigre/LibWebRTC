package org.webrtc.voiceengine;

import android.annotation.TargetApi;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Process;
import android.support.annotation.Nullable;
import java.nio.ByteBuffer;
import org.webrtc.ContextUtils;
import org.webrtc.Logging;
import org.webrtc.ThreadUtils;

public class WebRtcAudioTrack {
  private static final boolean DEBUG = false;
  
  private static final String TAG = "WebRtcAudioTrack";
  
  private static final int BITS_PER_SAMPLE = 16;
  
  private static final int CALLBACK_BUFFER_SIZE_MS = 10;
  
  private static final int BUFFERS_PER_SECOND = 100;
  
  private static final long AUDIO_TRACK_THREAD_JOIN_TIMEOUT_MS = 2000L;
  
  private static final int DEFAULT_USAGE = getDefaultUsageAttribute();
  
  private static int usageAttribute = DEFAULT_USAGE;
  
  private final long nativeAudioTrack;
  
  private final AudioManager audioManager;
  
  public static synchronized void setAudioTrackUsageAttribute(int usage) {
    Logging.w("WebRtcAudioTrack", "Default usage attribute is changed from: " + DEFAULT_USAGE + " to " + usage);
    usageAttribute = usage;
  }
  
  private static int getDefaultUsageAttribute() {
    if (Build.VERSION.SDK_INT >= 21)
      return 2; 
    return 0;
  }
  
  private final ThreadUtils.ThreadChecker threadChecker = new ThreadUtils.ThreadChecker();
  
  private ByteBuffer byteBuffer;
  
  @Nullable
  private AudioTrack audioTrack;
  
  @Nullable
  private AudioTrackThread audioThread;
  
  private static volatile boolean speakerMute;
  
  private byte[] emptyBytes;
  
  @Nullable
  private static WebRtcAudioTrackErrorCallback errorCallbackOld;
  
  @Nullable
  private static ErrorCallback errorCallback;
  
  public enum AudioTrackStartErrorCode {
    AUDIO_TRACK_START_EXCEPTION, AUDIO_TRACK_START_STATE_MISMATCH;
  }
  
  @Deprecated
  public static void setErrorCallback(WebRtcAudioTrackErrorCallback errorCallback) {
    Logging.d("WebRtcAudioTrack", "Set error callback (deprecated");
    errorCallbackOld = errorCallback;
  }
  
  public static void setErrorCallback(ErrorCallback errorCallback) {
    Logging.d("WebRtcAudioTrack", "Set extended error callback");
    WebRtcAudioTrack.errorCallback = errorCallback;
  }
  
  @Deprecated
  public static interface WebRtcAudioTrackErrorCallback {
    void onWebRtcAudioTrackInitError(String param1String);
    
    void onWebRtcAudioTrackStartError(String param1String);
    
    void onWebRtcAudioTrackError(String param1String);
  }
  
  public static interface ErrorCallback {
    void onWebRtcAudioTrackInitError(String param1String);
    
    void onWebRtcAudioTrackStartError(WebRtcAudioTrack.AudioTrackStartErrorCode param1AudioTrackStartErrorCode, String param1String);
    
    void onWebRtcAudioTrackError(String param1String);
  }
  
  private class AudioTrackThread extends Thread {
    private volatile boolean keepAlive = true;
    
    public AudioTrackThread(String name) {
      super(name);
    }
    
    public void run() {
      Process.setThreadPriority(-19);
      Logging.d("WebRtcAudioTrack", "AudioTrackThread" + WebRtcAudioUtils.getThreadInfo());
      WebRtcAudioTrack.assertTrue((WebRtcAudioTrack.this.audioTrack.getPlayState() == 3));
      int sizeInBytes = WebRtcAudioTrack.this.byteBuffer.capacity();
      while (this.keepAlive) {
        WebRtcAudioTrack.this.nativeGetPlayoutData(sizeInBytes, WebRtcAudioTrack.this.nativeAudioTrack);
        WebRtcAudioTrack.assertTrue((sizeInBytes <= WebRtcAudioTrack.this.byteBuffer.remaining()));
        if (WebRtcAudioTrack.speakerMute) {
          WebRtcAudioTrack.this.byteBuffer.clear();
          WebRtcAudioTrack.this.byteBuffer.put(WebRtcAudioTrack.this.emptyBytes);
          WebRtcAudioTrack.this.byteBuffer.position(0);
        } 
        int bytesWritten = writeBytes(WebRtcAudioTrack.this.audioTrack, WebRtcAudioTrack.this.byteBuffer, sizeInBytes);
        if (bytesWritten != sizeInBytes) {
          Logging.e("WebRtcAudioTrack", "AudioTrack.write played invalid number of bytes: " + bytesWritten);
          if (bytesWritten < 0) {
            this.keepAlive = false;
            WebRtcAudioTrack.this.reportWebRtcAudioTrackError("AudioTrack.write failed: " + bytesWritten);
          } 
        } 
        WebRtcAudioTrack.this.byteBuffer.rewind();
      } 
      if (WebRtcAudioTrack.this.audioTrack != null) {
        Logging.d("WebRtcAudioTrack", "Calling AudioTrack.stop...");
        try {
          WebRtcAudioTrack.this.audioTrack.stop();
          Logging.d("WebRtcAudioTrack", "AudioTrack.stop is done.");
        } catch (IllegalStateException e) {
          Logging.e("WebRtcAudioTrack", "AudioTrack.stop failed: " + e.getMessage());
        } 
      } 
    }
    
    private int writeBytes(AudioTrack audioTrack, ByteBuffer byteBuffer, int sizeInBytes) {
      if (Build.VERSION.SDK_INT >= 21)
        return audioTrack.write(byteBuffer, sizeInBytes, 0); 
      return audioTrack.write(byteBuffer.array(), byteBuffer.arrayOffset(), sizeInBytes);
    }
    
    public void stopThread() {
      Logging.d("WebRtcAudioTrack", "stopThread");
      this.keepAlive = false;
    }
  }
  
  WebRtcAudioTrack(long nativeAudioTrack) {
    this.threadChecker.checkIsOnValidThread();
    Logging.d("WebRtcAudioTrack", "ctor" + WebRtcAudioUtils.getThreadInfo());
    this.nativeAudioTrack = nativeAudioTrack;
    this
      .audioManager = (AudioManager)ContextUtils.getApplicationContext().getSystemService("audio");
  }
  
  private boolean initPlayout(int sampleRate, int channels, double bufferSizeFactor) {
    this.threadChecker.checkIsOnValidThread();
    Logging.d("WebRtcAudioTrack", "initPlayout(sampleRate=" + sampleRate + ", channels=" + channels + ", bufferSizeFactor=" + bufferSizeFactor + ")");
    int bytesPerFrame = channels * 2;
    this.byteBuffer = ByteBuffer.allocateDirect(bytesPerFrame * sampleRate / 100);
    Logging.d("WebRtcAudioTrack", "byteBuffer.capacity: " + this.byteBuffer.capacity());
    this.emptyBytes = new byte[this.byteBuffer.capacity()];
    nativeCacheDirectBufferAddress(this.byteBuffer, this.nativeAudioTrack);
    int channelConfig = channelCountToConfiguration(channels);
    int minBufferSizeInBytes = (int)(AudioTrack.getMinBufferSize(sampleRate, channelConfig, 2) * bufferSizeFactor);
    Logging.d("WebRtcAudioTrack", "minBufferSizeInBytes: " + minBufferSizeInBytes);
    if (minBufferSizeInBytes < this.byteBuffer.capacity()) {
      reportWebRtcAudioTrackInitError("AudioTrack.getMinBufferSize returns an invalid value.");
      return false;
    } 
    if (this.audioTrack != null) {
      reportWebRtcAudioTrackInitError("Conflict with existing AudioTrack.");
      return false;
    } 
    try {
      if (Build.VERSION.SDK_INT >= 21) {
        this.audioTrack = createAudioTrackOnLollipopOrHigher(sampleRate, channelConfig, minBufferSizeInBytes);
      } else {
        this
          .audioTrack = createAudioTrackOnLowerThanLollipop(sampleRate, channelConfig, minBufferSizeInBytes);
      } 
    } catch (IllegalArgumentException e) {
      reportWebRtcAudioTrackInitError(e.getMessage());
      releaseAudioResources();
      return false;
    } 
    if (this.audioTrack == null || this.audioTrack.getState() != 1) {
      reportWebRtcAudioTrackInitError("Initialization of audio track failed.");
      releaseAudioResources();
      return false;
    } 
    logMainParameters();
    logMainParametersExtended();
    return true;
  }
  
  private boolean startPlayout() {
    this.threadChecker.checkIsOnValidThread();
    Logging.d("WebRtcAudioTrack", "startPlayout");
    assertTrue((this.audioTrack != null));
    assertTrue((this.audioThread == null));
    try {
      this.audioTrack.play();
    } catch (IllegalStateException e) {
      reportWebRtcAudioTrackStartError(AudioTrackStartErrorCode.AUDIO_TRACK_START_EXCEPTION, "AudioTrack.play failed: " + e
          .getMessage());
      releaseAudioResources();
      return false;
    } 
    if (this.audioTrack.getPlayState() != 3) {
      reportWebRtcAudioTrackStartError(AudioTrackStartErrorCode.AUDIO_TRACK_START_STATE_MISMATCH, "AudioTrack.play failed - incorrect state :" + this.audioTrack
          
          .getPlayState());
      releaseAudioResources();
      return false;
    } 
    this.audioThread = new AudioTrackThread("AudioTrackJavaThread");
    this.audioThread.start();
    return true;
  }
  
  private boolean stopPlayout() {
    this.threadChecker.checkIsOnValidThread();
    Logging.d("WebRtcAudioTrack", "stopPlayout");
    assertTrue((this.audioThread != null));
    logUnderrunCount();
    this.audioThread.stopThread();
    Logging.d("WebRtcAudioTrack", "Stopping the AudioTrackThread...");
    this.audioThread.interrupt();
    if (!ThreadUtils.joinUninterruptibly(this.audioThread, 2000L)) {
      Logging.e("WebRtcAudioTrack", "Join of AudioTrackThread timed out.");
      WebRtcAudioUtils.logAudioState("WebRtcAudioTrack");
    } 
    Logging.d("WebRtcAudioTrack", "AudioTrackThread has now been stopped.");
    this.audioThread = null;
    releaseAudioResources();
    return true;
  }
  
  private int getStreamMaxVolume() {
    this.threadChecker.checkIsOnValidThread();
    Logging.d("WebRtcAudioTrack", "getStreamMaxVolume");
    assertTrue((this.audioManager != null));
    return this.audioManager.getStreamMaxVolume(0);
  }
  
  private boolean setStreamVolume(int volume) {
    this.threadChecker.checkIsOnValidThread();
    Logging.d("WebRtcAudioTrack", "setStreamVolume(" + volume + ")");
    assertTrue((this.audioManager != null));
    if (isVolumeFixed()) {
      Logging.e("WebRtcAudioTrack", "The device implements a fixed volume policy.");
      return false;
    } 
    this.audioManager.setStreamVolume(0, volume, 0);
    return true;
  }
  
  private boolean isVolumeFixed() {
    if (Build.VERSION.SDK_INT < 21)
      return false; 
    return this.audioManager.isVolumeFixed();
  }
  
  private int getStreamVolume() {
    this.threadChecker.checkIsOnValidThread();
    Logging.d("WebRtcAudioTrack", "getStreamVolume");
    assertTrue((this.audioManager != null));
    return this.audioManager.getStreamVolume(0);
  }
  
  private void logMainParameters() {
    Logging.d("WebRtcAudioTrack", "AudioTrack: session ID: " + this.audioTrack
        .getAudioSessionId() + ", channels: " + this.audioTrack
        .getChannelCount() + ", sample rate: " + this.audioTrack
        .getSampleRate() + ", max gain: " + 
        
        AudioTrack.getMaxVolume());
  }
  
  @TargetApi(21)
  private static AudioTrack createAudioTrackOnLollipopOrHigher(int sampleRateInHz, int channelConfig, int bufferSizeInBytes) {
    Logging.d("WebRtcAudioTrack", "createAudioTrackOnLollipopOrHigher");
    int nativeOutputSampleRate = AudioTrack.getNativeOutputSampleRate(0);
    Logging.d("WebRtcAudioTrack", "nativeOutputSampleRate: " + nativeOutputSampleRate);
    if (sampleRateInHz != nativeOutputSampleRate)
      Logging.w("WebRtcAudioTrack", "Unable to use fast mode since requested sample rate is not native"); 
    if (usageAttribute != DEFAULT_USAGE)
      Logging.w("WebRtcAudioTrack", "A non default usage attribute is used: " + usageAttribute); 
    return new AudioTrack((new AudioAttributes.Builder())
        
        .setUsage(usageAttribute)
        .setContentType(1)
        .build(), (new AudioFormat.Builder())
        
        .setEncoding(2)
        .setSampleRate(sampleRateInHz)
        .setChannelMask(channelConfig)
        .build(), bufferSizeInBytes, 1, 0);
  }
  
  private static AudioTrack createAudioTrackOnLowerThanLollipop(int sampleRateInHz, int channelConfig, int bufferSizeInBytes) {
    return new AudioTrack(0, sampleRateInHz, channelConfig, 2, bufferSizeInBytes, 1);
  }
  
  private void logBufferSizeInFrames() {
    if (Build.VERSION.SDK_INT >= 23)
      Logging.d("WebRtcAudioTrack", "AudioTrack: buffer size in frames: " + this.audioTrack
          
          .getBufferSizeInFrames()); 
  }
  
  private void logBufferCapacityInFrames() {
    if (Build.VERSION.SDK_INT >= 24)
      Logging.d("WebRtcAudioTrack", "AudioTrack: buffer capacity in frames: " + this.audioTrack
          
          .getBufferCapacityInFrames()); 
  }
  
  private void logMainParametersExtended() {
    logBufferSizeInFrames();
    logBufferCapacityInFrames();
  }
  
  private void logUnderrunCount() {
    if (Build.VERSION.SDK_INT >= 24)
      Logging.d("WebRtcAudioTrack", "underrun count: " + this.audioTrack.getUnderrunCount()); 
  }
  
  private static void assertTrue(boolean condition) {
    if (!condition)
      throw new AssertionError("Expected condition to be true"); 
  }
  
  private int channelCountToConfiguration(int channels) {
    return (channels == 1) ? 4 : 12;
  }
  
  public static void setSpeakerMute(boolean mute) {
    Logging.w("WebRtcAudioTrack", "setSpeakerMute(" + mute + ")");
    speakerMute = mute;
  }
  
  private void releaseAudioResources() {
    Logging.d("WebRtcAudioTrack", "releaseAudioResources");
    if (this.audioTrack != null) {
      this.audioTrack.release();
      this.audioTrack = null;
    } 
  }
  
  private void reportWebRtcAudioTrackInitError(String errorMessage) {
    Logging.e("WebRtcAudioTrack", "Init playout error: " + errorMessage);
    WebRtcAudioUtils.logAudioState("WebRtcAudioTrack");
    if (errorCallbackOld != null)
      errorCallbackOld.onWebRtcAudioTrackInitError(errorMessage); 
    if (errorCallback != null)
      errorCallback.onWebRtcAudioTrackInitError(errorMessage); 
  }
  
  private void reportWebRtcAudioTrackStartError(AudioTrackStartErrorCode errorCode, String errorMessage) {
    Logging.e("WebRtcAudioTrack", "Start playout error: " + errorCode + ". " + errorMessage);
    WebRtcAudioUtils.logAudioState("WebRtcAudioTrack");
    if (errorCallbackOld != null)
      errorCallbackOld.onWebRtcAudioTrackStartError(errorMessage); 
    if (errorCallback != null)
      errorCallback.onWebRtcAudioTrackStartError(errorCode, errorMessage); 
  }
  
  private void reportWebRtcAudioTrackError(String errorMessage) {
    Logging.e("WebRtcAudioTrack", "Run-time playback error: " + errorMessage);
    WebRtcAudioUtils.logAudioState("WebRtcAudioTrack");
    if (errorCallbackOld != null)
      errorCallbackOld.onWebRtcAudioTrackError(errorMessage); 
    if (errorCallback != null)
      errorCallback.onWebRtcAudioTrackError(errorMessage); 
  }
  
  private native void nativeCacheDirectBufferAddress(ByteBuffer paramByteBuffer, long paramLong);
  
  private native void nativeGetPlayoutData(int paramInt, long paramLong);
}
