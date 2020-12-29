package org.webrtc.audio;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.support.annotation.RequiresApi;
import org.webrtc.JniCommon;
import org.webrtc.Logging;

public class JavaAudioDeviceModule implements AudioDeviceModule {
  private static final String TAG = "JavaAudioDeviceModule";
  
  private final Context context;
  
  private final AudioManager audioManager;
  
  private final WebRtcAudioRecord audioInput;
  
  private final WebRtcAudioTrack audioOutput;
  
  private final int inputSampleRate;
  
  private final int outputSampleRate;
  
  private final boolean useStereoInput;
  
  private final boolean useStereoOutput;
  
  public static Builder builder(Context context) {
    return new Builder(context);
  }
  
  public static class Builder {
    private final Context context;
    
    private final AudioManager audioManager;
    
    private int inputSampleRate;
    
    private int outputSampleRate;
    
    private int audioSource = 7;
    
    private int audioFormat = 2;
    
    private JavaAudioDeviceModule.AudioTrackErrorCallback audioTrackErrorCallback;
    
    private JavaAudioDeviceModule.AudioRecordErrorCallback audioRecordErrorCallback;
    
    private JavaAudioDeviceModule.SamplesReadyCallback samplesReadyCallback;
    
    private JavaAudioDeviceModule.AudioTrackStateCallback audioTrackStateCallback;
    
    private JavaAudioDeviceModule.AudioRecordStateCallback audioRecordStateCallback;
    
    private boolean useHardwareAcousticEchoCanceler = JavaAudioDeviceModule.isBuiltInAcousticEchoCancelerSupported();
    
    private boolean useHardwareNoiseSuppressor = JavaAudioDeviceModule.isBuiltInNoiseSuppressorSupported();
    
    private boolean useStereoInput;
    
    private boolean useStereoOutput;
    
    private Builder(Context context) {
      this.context = context;
      this.audioManager = (AudioManager)context.getSystemService("audio");
      this.inputSampleRate = WebRtcAudioManager.getSampleRate(this.audioManager);
      this.outputSampleRate = WebRtcAudioManager.getSampleRate(this.audioManager);
    }
    
    public Builder setSampleRate(int sampleRate) {
      Logging.d("JavaAudioDeviceModule", "Input/Output sample rate overridden to: " + sampleRate);
      this.inputSampleRate = sampleRate;
      this.outputSampleRate = sampleRate;
      return this;
    }
    
    public Builder setInputSampleRate(int inputSampleRate) {
      Logging.d("JavaAudioDeviceModule", "Input sample rate overridden to: " + inputSampleRate);
      this.inputSampleRate = inputSampleRate;
      return this;
    }
    
    public Builder setOutputSampleRate(int outputSampleRate) {
      Logging.d("JavaAudioDeviceModule", "Output sample rate overridden to: " + outputSampleRate);
      this.outputSampleRate = outputSampleRate;
      return this;
    }
    
    public Builder setAudioSource(int audioSource) {
      this.audioSource = audioSource;
      return this;
    }
    
    public Builder setAudioFormat(int audioFormat) {
      this.audioFormat = audioFormat;
      return this;
    }
    
    public Builder setAudioTrackErrorCallback(JavaAudioDeviceModule.AudioTrackErrorCallback audioTrackErrorCallback) {
      this.audioTrackErrorCallback = audioTrackErrorCallback;
      return this;
    }
    
    public Builder setAudioRecordErrorCallback(JavaAudioDeviceModule.AudioRecordErrorCallback audioRecordErrorCallback) {
      this.audioRecordErrorCallback = audioRecordErrorCallback;
      return this;
    }
    
    public Builder setSamplesReadyCallback(JavaAudioDeviceModule.SamplesReadyCallback samplesReadyCallback) {
      this.samplesReadyCallback = samplesReadyCallback;
      return this;
    }
    
    public Builder setAudioTrackStateCallback(JavaAudioDeviceModule.AudioTrackStateCallback audioTrackStateCallback) {
      this.audioTrackStateCallback = audioTrackStateCallback;
      return this;
    }
    
    public Builder setAudioRecordStateCallback(JavaAudioDeviceModule.AudioRecordStateCallback audioRecordStateCallback) {
      this.audioRecordStateCallback = audioRecordStateCallback;
      return this;
    }
    
    public Builder setUseHardwareNoiseSuppressor(boolean useHardwareNoiseSuppressor) {
      if (useHardwareNoiseSuppressor && !JavaAudioDeviceModule.isBuiltInNoiseSuppressorSupported()) {
        Logging.e("JavaAudioDeviceModule", "HW NS not supported");
        useHardwareNoiseSuppressor = false;
      } 
      this.useHardwareNoiseSuppressor = useHardwareNoiseSuppressor;
      return this;
    }
    
    public Builder setUseHardwareAcousticEchoCanceler(boolean useHardwareAcousticEchoCanceler) {
      if (useHardwareAcousticEchoCanceler && !JavaAudioDeviceModule.isBuiltInAcousticEchoCancelerSupported()) {
        Logging.e("JavaAudioDeviceModule", "HW AEC not supported");
        useHardwareAcousticEchoCanceler = false;
      } 
      this.useHardwareAcousticEchoCanceler = useHardwareAcousticEchoCanceler;
      return this;
    }
    
    public Builder setUseStereoInput(boolean useStereoInput) {
      this.useStereoInput = useStereoInput;
      return this;
    }
    
    public Builder setUseStereoOutput(boolean useStereoOutput) {
      this.useStereoOutput = useStereoOutput;
      return this;
    }
    
    public AudioDeviceModule createAudioDeviceModule() {
      Logging.d("JavaAudioDeviceModule", "createAudioDeviceModule");
      if (this.useHardwareNoiseSuppressor) {
        Logging.d("JavaAudioDeviceModule", "HW NS will be used.");
      } else {
        if (JavaAudioDeviceModule.isBuiltInNoiseSuppressorSupported())
          Logging.d("JavaAudioDeviceModule", "Overriding default behavior; now using WebRTC NS!"); 
        Logging.d("JavaAudioDeviceModule", "HW NS will not be used.");
      } 
      if (this.useHardwareAcousticEchoCanceler) {
        Logging.d("JavaAudioDeviceModule", "HW AEC will be used.");
      } else {
        if (JavaAudioDeviceModule.isBuiltInAcousticEchoCancelerSupported())
          Logging.d("JavaAudioDeviceModule", "Overriding default behavior; now using WebRTC AEC!"); 
        Logging.d("JavaAudioDeviceModule", "HW AEC will not be used.");
      } 
      WebRtcAudioRecord audioInput = new WebRtcAudioRecord(this.context, this.audioManager, this.audioSource, this.audioFormat, this.audioRecordErrorCallback, this.audioRecordStateCallback, this.samplesReadyCallback, this.useHardwareAcousticEchoCanceler, this.useHardwareNoiseSuppressor);
      WebRtcAudioTrack audioOutput = new WebRtcAudioTrack(this.context, this.audioManager, this.audioTrackErrorCallback, this.audioTrackStateCallback);
      return new JavaAudioDeviceModule(this.context, this.audioManager, audioInput, audioOutput, this.inputSampleRate, this.outputSampleRate, this.useStereoInput, this.useStereoOutput);
    }
  }
  
  public enum AudioRecordStartErrorCode {
    AUDIO_RECORD_START_EXCEPTION, AUDIO_RECORD_START_STATE_MISMATCH;
  }
  
  public static interface AudioRecordErrorCallback {
    void onWebRtcAudioRecordInitError(String param1String);
    
    void onWebRtcAudioRecordStartError(JavaAudioDeviceModule.AudioRecordStartErrorCode param1AudioRecordStartErrorCode, String param1String);
    
    void onWebRtcAudioRecordError(String param1String);
  }
  
  public static interface AudioRecordStateCallback {
    void onWebRtcAudioRecordStart();
    
    void onWebRtcAudioRecordStop();
  }
  
  public static class AudioSamples {
    private final int audioFormat;
    
    private final int channelCount;
    
    private final int sampleRate;
    
    private final byte[] data;
    
    public AudioSamples(int audioFormat, int channelCount, int sampleRate, byte[] data) {
      this.audioFormat = audioFormat;
      this.channelCount = channelCount;
      this.sampleRate = sampleRate;
      this.data = data;
    }
    
    public int getAudioFormat() {
      return this.audioFormat;
    }
    
    public int getChannelCount() {
      return this.channelCount;
    }
    
    public int getSampleRate() {
      return this.sampleRate;
    }
    
    public byte[] getData() {
      return this.data;
    }
  }
  
  public static interface SamplesReadyCallback {
    void onWebRtcAudioRecordSamplesReady(JavaAudioDeviceModule.AudioSamples param1AudioSamples);
  }
  
  public enum AudioTrackStartErrorCode {
    AUDIO_TRACK_START_EXCEPTION, AUDIO_TRACK_START_STATE_MISMATCH;
  }
  
  public static boolean isBuiltInAcousticEchoCancelerSupported() {
    return WebRtcAudioEffects.isAcousticEchoCancelerSupported();
  }
  
  public static boolean isBuiltInNoiseSuppressorSupported() {
    return WebRtcAudioEffects.isNoiseSuppressorSupported();
  }
  
  private final Object nativeLock = new Object();
  
  private long nativeAudioDeviceModule;
  
  private JavaAudioDeviceModule(Context context, AudioManager audioManager, WebRtcAudioRecord audioInput, WebRtcAudioTrack audioOutput, int inputSampleRate, int outputSampleRate, boolean useStereoInput, boolean useStereoOutput) {
    this.context = context;
    this.audioManager = audioManager;
    this.audioInput = audioInput;
    this.audioOutput = audioOutput;
    this.inputSampleRate = inputSampleRate;
    this.outputSampleRate = outputSampleRate;
    this.useStereoInput = useStereoInput;
    this.useStereoOutput = useStereoOutput;
  }
  
  public long getNativeAudioDeviceModulePointer() {
    synchronized (this.nativeLock) {
      if (this.nativeAudioDeviceModule == 0L)
        this.nativeAudioDeviceModule = nativeCreateAudioDeviceModule(this.context, this.audioManager, this.audioInput, this.audioOutput, this.inputSampleRate, this.outputSampleRate, this.useStereoInput, this.useStereoOutput); 
      return this.nativeAudioDeviceModule;
    } 
  }
  
  public void release() {
    synchronized (this.nativeLock) {
      if (this.nativeAudioDeviceModule != 0L) {
        JniCommon.nativeReleaseRef(this.nativeAudioDeviceModule);
        this.nativeAudioDeviceModule = 0L;
      } 
    } 
  }
  
  public void setSpeakerMute(boolean mute) {
    Logging.d("JavaAudioDeviceModule", "setSpeakerMute: " + mute);
    this.audioOutput.setSpeakerMute(mute);
  }
  
  public void setMicrophoneMute(boolean mute) {
    Logging.d("JavaAudioDeviceModule", "setMicrophoneMute: " + mute);
    this.audioInput.setMicrophoneMute(mute);
  }
  
  @RequiresApi(23)
  public void setPreferredInputDevice(AudioDeviceInfo preferredInputDevice) {
    Logging.d("JavaAudioDeviceModule", "setPreferredInputDevice: " + preferredInputDevice);
    this.audioInput.setPreferredDevice(preferredInputDevice);
  }
  
  private static native long nativeCreateAudioDeviceModule(Context paramContext, AudioManager paramAudioManager, WebRtcAudioRecord paramWebRtcAudioRecord, WebRtcAudioTrack paramWebRtcAudioTrack, int paramInt1, int paramInt2, boolean paramBoolean1, boolean paramBoolean2);
  
  public static interface AudioTrackErrorCallback {
    void onWebRtcAudioTrackInitError(String param1String);
    
    void onWebRtcAudioTrackStartError(JavaAudioDeviceModule.AudioTrackStartErrorCode param1AudioTrackStartErrorCode, String param1String);
    
    void onWebRtcAudioTrackError(String param1String);
  }
  
  public static interface AudioTrackStateCallback {
    void onWebRtcAudioTrackStart();
    
    void onWebRtcAudioTrackStop();
  }
}
