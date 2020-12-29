package org.webrtc.audio;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Process;
import android.support.annotation.Nullable;
import java.nio.ByteBuffer;
import org.webrtc.CalledByNative;
import org.webrtc.Logging;
import org.webrtc.ThreadUtils;

class WebRtcAudioTrack {
  private static final String TAG = "WebRtcAudioTrackExternal";
  
  private static final int BITS_PER_SAMPLE = 16;
  
  private static final int CALLBACK_BUFFER_SIZE_MS = 10;
  
  private static final int BUFFERS_PER_SECOND = 100;
  
  private static final long AUDIO_TRACK_THREAD_JOIN_TIMEOUT_MS = 2000L;
  
  private static final int DEFAULT_USAGE = getDefaultUsageAttribute();
  
  private static final int AUDIO_TRACK_START = 0;
  
  private static final int AUDIO_TRACK_STOP = 1;
  
  private long nativeAudioTrack;
  
  private final Context context;
  
  private final AudioManager audioManager;
  
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
  
  private final VolumeLogger volumeLogger;
  
  private volatile boolean speakerMute;
  
  private byte[] emptyBytes;
  
  @Nullable
  private final JavaAudioDeviceModule.AudioTrackErrorCallback errorCallback;
  
  @Nullable
  private final JavaAudioDeviceModule.AudioTrackStateCallback stateCallback;
  
  private class AudioTrackThread extends Thread {
    private volatile boolean keepAlive = true;
    
    public AudioTrackThread(String name) {
      super(name);
    }
    
    public void run() {
      Process.setThreadPriority(-19);
      Logging.d("WebRtcAudioTrackExternal", "AudioTrackThread" + WebRtcAudioUtils.getThreadInfo());
      WebRtcAudioTrack.assertTrue((WebRtcAudioTrack.this.audioTrack.getPlayState() == 3));
      WebRtcAudioTrack.this.doAudioTrackStateCallback(0);
      int sizeInBytes = WebRtcAudioTrack.this.byteBuffer.capacity();
      while (this.keepAlive) {
        WebRtcAudioTrack.nativeGetPlayoutData(WebRtcAudioTrack.this.nativeAudioTrack, sizeInBytes);
        WebRtcAudioTrack.assertTrue((sizeInBytes <= WebRtcAudioTrack.this.byteBuffer.remaining()));
        if (WebRtcAudioTrack.this.speakerMute) {
          WebRtcAudioTrack.this.byteBuffer.clear();
          WebRtcAudioTrack.this.byteBuffer.put(WebRtcAudioTrack.this.emptyBytes);
          WebRtcAudioTrack.this.byteBuffer.position(0);
        } 
        int bytesWritten = writeBytes(WebRtcAudioTrack.this.audioTrack, WebRtcAudioTrack.this.byteBuffer, sizeInBytes);
        if (bytesWritten != sizeInBytes) {
          Logging.e("WebRtcAudioTrackExternal", "AudioTrack.write played invalid number of bytes: " + bytesWritten);
          if (bytesWritten < 0) {
            this.keepAlive = false;
            WebRtcAudioTrack.this.reportWebRtcAudioTrackError("AudioTrack.write failed: " + bytesWritten);
          } 
        } 
        WebRtcAudioTrack.this.byteBuffer.rewind();
      } 
    }
    
    private int writeBytes(AudioTrack audioTrack, ByteBuffer byteBuffer, int sizeInBytes) {
      if (Build.VERSION.SDK_INT >= 21)
        return audioTrack.write(byteBuffer, sizeInBytes, 0); 
      return audioTrack.write(byteBuffer.array(), byteBuffer.arrayOffset(), sizeInBytes);
    }
    
    public void stopThread() {
      Logging.d("WebRtcAudioTrackExternal", "stopThread");
      this.keepAlive = false;
    }
  }
  
  @CalledByNative
  WebRtcAudioTrack(Context context, AudioManager audioManager) {
    this(context, audioManager, null, null);
  }
  
  WebRtcAudioTrack(Context context, AudioManager audioManager, @Nullable JavaAudioDeviceModule.AudioTrackErrorCallback errorCallback, @Nullable JavaAudioDeviceModule.AudioTrackStateCallback stateCallback) {
    this.threadChecker.detachThread();
    this.context = context;
    this.audioManager = audioManager;
    this.errorCallback = errorCallback;
    this.stateCallback = stateCallback;
    this.volumeLogger = new VolumeLogger(audioManager);
    Logging.d("WebRtcAudioTrackExternal", "ctor" + WebRtcAudioUtils.getThreadInfo());
  }
  
  @CalledByNative
  public void setNativeAudioTrack(long nativeAudioTrack) {
    this.nativeAudioTrack = nativeAudioTrack;
  }
  
  @CalledByNative
  private boolean initPlayout(int sampleRate, int channels, double bufferSizeFactor) {
    this.threadChecker.checkIsOnValidThread();
    Logging.d("WebRtcAudioTrackExternal", "initPlayout(sampleRate=" + sampleRate + ", channels=" + channels + ", bufferSizeFactor=" + bufferSizeFactor + ")");
    int bytesPerFrame = channels * 2;
    this.byteBuffer = ByteBuffer.allocateDirect(bytesPerFrame * sampleRate / 100);
    Logging.d("WebRtcAudioTrackExternal", "byteBuffer.capacity: " + this.byteBuffer.capacity());
    this.emptyBytes = new byte[this.byteBuffer.capacity()];
    nativeCacheDirectBufferAddress(this.nativeAudioTrack, this.byteBuffer);
    int channelConfig = channelCountToConfiguration(channels);
    int minBufferSizeInBytes = (int)(AudioTrack.getMinBufferSize(sampleRate, channelConfig, 2) * bufferSizeFactor);
    Logging.d("WebRtcAudioTrackExternal", "minBufferSizeInBytes: " + minBufferSizeInBytes);
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
        this
          .audioTrack = createAudioTrackOnLollipopOrHigher(sampleRate, channelConfig, minBufferSizeInBytes);
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
  
  @CalledByNative
  private boolean startPlayout() {
    this.threadChecker.checkIsOnValidThread();
    this.volumeLogger.start();
    Logging.d("WebRtcAudioTrackExternal", "startPlayout");
    assertTrue((this.audioTrack != null));
    assertTrue((this.audioThread == null));
    try {
      this.audioTrack.play();
    } catch (IllegalStateException e) {
      reportWebRtcAudioTrackStartError(JavaAudioDeviceModule.AudioTrackStartErrorCode.AUDIO_TRACK_START_EXCEPTION, "AudioTrack.play failed: " + e
          .getMessage());
      releaseAudioResources();
      return false;
    } 
    if (this.audioTrack.getPlayState() != 3) {
      reportWebRtcAudioTrackStartError(JavaAudioDeviceModule.AudioTrackStartErrorCode.AUDIO_TRACK_START_STATE_MISMATCH, "AudioTrack.play failed - incorrect state :" + this.audioTrack
          .getPlayState());
      releaseAudioResources();
      return false;
    } 
    this.audioThread = new AudioTrackThread("AudioTrackJavaThread");
    this.audioThread.start();
    return true;
  }
  
  @CalledByNative
  private boolean stopPlayout() {
    this.threadChecker.checkIsOnValidThread();
    this.volumeLogger.stop();
    Logging.d("WebRtcAudioTrackExternal", "stopPlayout");
    assertTrue((this.audioThread != null));
    logUnderrunCount();
    this.audioThread.stopThread();
    Logging.d("WebRtcAudioTrackExternal", "Stopping the AudioTrackThread...");
    this.audioThread.interrupt();
    if (!ThreadUtils.joinUninterruptibly(this.audioThread, 2000L)) {
      Logging.e("WebRtcAudioTrackExternal", "Join of AudioTrackThread timed out.");
      WebRtcAudioUtils.logAudioState("WebRtcAudioTrackExternal", this.context, this.audioManager);
    } 
    Logging.d("WebRtcAudioTrackExternal", "AudioTrackThread has now been stopped.");
    this.audioThread = null;
    if (this.audioTrack != null) {
      Logging.d("WebRtcAudioTrackExternal", "Calling AudioTrack.stop...");
      try {
        this.audioTrack.stop();
        Logging.d("WebRtcAudioTrackExternal", "AudioTrack.stop is done.");
        doAudioTrackStateCallback(1);
      } catch (IllegalStateException e) {
        Logging.e("WebRtcAudioTrackExternal", "AudioTrack.stop failed: " + e.getMessage());
      } 
    } 
    releaseAudioResources();
    return true;
  }
  
  @CalledByNative
  private int getStreamMaxVolume() {
    this.threadChecker.checkIsOnValidThread();
    Logging.d("WebRtcAudioTrackExternal", "getStreamMaxVolume");
    return this.audioManager.getStreamMaxVolume(0);
  }
  
  @CalledByNative
  private boolean setStreamVolume(int volume) {
    this.threadChecker.checkIsOnValidThread();
    Logging.d("WebRtcAudioTrackExternal", "setStreamVolume(" + volume + ")");
    if (isVolumeFixed()) {
      Logging.e("WebRtcAudioTrackExternal", "The device implements a fixed volume policy.");
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
  
  @CalledByNative
  private int getStreamVolume() {
    this.threadChecker.checkIsOnValidThread();
    Logging.d("WebRtcAudioTrackExternal", "getStreamVolume");
    return this.audioManager.getStreamVolume(0);
  }
  
  @CalledByNative
  private int GetPlayoutUnderrunCount() {
    if (Build.VERSION.SDK_INT >= 24) {
      if (this.audioTrack != null)
        return this.audioTrack.getUnderrunCount(); 
      return -1;
    } 
    return -2;
  }
  
  private void logMainParameters() {
    Logging.d("WebRtcAudioTrackExternal", "AudioTrack: session ID: " + this.audioTrack
        
        .getAudioSessionId() + ", channels: " + this.audioTrack
        .getChannelCount() + ", sample rate: " + this.audioTrack
        .getSampleRate() + ", max gain: " + 
        
        AudioTrack.getMaxVolume());
  }
  
  @TargetApi(21)
  private static AudioTrack createAudioTrackOnLollipopOrHigher(int sampleRateInHz, int channelConfig, int bufferSizeInBytes) {
    Logging.d("WebRtcAudioTrackExternal", "createAudioTrackOnLollipopOrHigher");
    int nativeOutputSampleRate = AudioTrack.getNativeOutputSampleRate(0);
    Logging.d("WebRtcAudioTrackExternal", "nativeOutputSampleRate: " + nativeOutputSampleRate);
    if (sampleRateInHz != nativeOutputSampleRate)
      Logging.w("WebRtcAudioTrackExternal", "Unable to use fast mode since requested sample rate is not native"); 
    return new AudioTrack((new AudioAttributes.Builder())
        .setUsage(DEFAULT_USAGE)
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
      Logging.d("WebRtcAudioTrackExternal", "AudioTrack: buffer size in frames: " + this.audioTrack
          
          .getBufferSizeInFrames()); 
  }
  
  private void logBufferCapacityInFrames() {
    if (Build.VERSION.SDK_INT >= 24)
      Logging.d("WebRtcAudioTrackExternal", "AudioTrack: buffer capacity in frames: " + this.audioTrack
          
          .getBufferCapacityInFrames()); 
  }
  
  private void logMainParametersExtended() {
    logBufferSizeInFrames();
    logBufferCapacityInFrames();
  }
  
  private void logUnderrunCount() {
    if (Build.VERSION.SDK_INT >= 24)
      Logging.d("WebRtcAudioTrackExternal", "underrun count: " + this.audioTrack.getUnderrunCount()); 
  }
  
  private static void assertTrue(boolean condition) {
    if (!condition)
      throw new AssertionError("Expected condition to be true"); 
  }
  
  private int channelCountToConfiguration(int channels) {
    return (channels == 1) ? 4 : 12;
  }
  
  public void setSpeakerMute(boolean mute) {
    Logging.w("WebRtcAudioTrackExternal", "setSpeakerMute(" + mute + ")");
    this.speakerMute = mute;
  }
  
  private void releaseAudioResources() {
    Logging.d("WebRtcAudioTrackExternal", "releaseAudioResources");
    if (this.audioTrack != null) {
      this.audioTrack.release();
      this.audioTrack = null;
    } 
  }
  
  private void reportWebRtcAudioTrackInitError(String errorMessage) {
    Logging.e("WebRtcAudioTrackExternal", "Init playout error: " + errorMessage);
    WebRtcAudioUtils.logAudioState("WebRtcAudioTrackExternal", this.context, this.audioManager);
    if (this.errorCallback != null)
      this.errorCallback.onWebRtcAudioTrackInitError(errorMessage); 
  }
  
  private void reportWebRtcAudioTrackStartError(JavaAudioDeviceModule.AudioTrackStartErrorCode errorCode, String errorMessage) {
    Logging.e("WebRtcAudioTrackExternal", "Start playout error: " + errorCode + ". " + errorMessage);
    WebRtcAudioUtils.logAudioState("WebRtcAudioTrackExternal", this.context, this.audioManager);
    if (this.errorCallback != null)
      this.errorCallback.onWebRtcAudioTrackStartError(errorCode, errorMessage); 
  }
  
  private void reportWebRtcAudioTrackError(String errorMessage) {
    Logging.e("WebRtcAudioTrackExternal", "Run-time playback error: " + errorMessage);
    WebRtcAudioUtils.logAudioState("WebRtcAudioTrackExternal", this.context, this.audioManager);
    if (this.errorCallback != null)
      this.errorCallback.onWebRtcAudioTrackError(errorMessage); 
  }
  
  private void doAudioTrackStateCallback(int audioState) {
    Logging.d("WebRtcAudioTrackExternal", "doAudioTrackStateCallback: " + audioState);
    if (this.stateCallback != null)
      if (audioState == 0) {
        this.stateCallback.onWebRtcAudioTrackStart();
      } else if (audioState == 1) {
        this.stateCallback.onWebRtcAudioTrackStop();
      } else {
        Logging.e("WebRtcAudioTrackExternal", "Invalid audio state");
      }  
  }
  
  private static native void nativeCacheDirectBufferAddress(long paramLong, ByteBuffer paramByteBuffer);
  
  private static native void nativeGetPlayoutData(long paramLong, int paramInt);
}
