package org.webrtc;

import android.content.Context;
import android.os.Process;
import android.support.annotation.Nullable;
import java.util.List;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;

public class PeerConnectionFactory {
  public static final String TRIAL_ENABLED = "Enabled";
  
  @Deprecated
  public static final String VIDEO_FRAME_EMIT_TRIAL = "VideoFrameEmit";
  
  private static final String TAG = "PeerConnectionFactory";
  
  private static final String VIDEO_CAPTURER_THREAD_NAME = "VideoCapturerThread";
  
  private static volatile boolean internalTracerInitialized;
  
  @Nullable
  private static ThreadInfo staticNetworkThread;
  
  @Nullable
  private static ThreadInfo staticWorkerThread;
  
  @Nullable
  private static ThreadInfo staticSignalingThread;
  
  private long nativeFactory;
  
  @Nullable
  private volatile ThreadInfo networkThread;
  
  @Nullable
  private volatile ThreadInfo workerThread;
  
  @Nullable
  private volatile ThreadInfo signalingThread;
  
  private static class ThreadInfo {
    final Thread thread;
    
    final int tid;
    
    public static ThreadInfo getCurrent() {
      return new ThreadInfo(Thread.currentThread(), Process.myTid());
    }
    
    private ThreadInfo(Thread thread, int tid) {
      this.thread = thread;
      this.tid = tid;
    }
  }
  
  public static class InitializationOptions {
    final Context applicationContext;
    
    final String fieldTrials;
    
    final boolean enableInternalTracer;
    
    final NativeLibraryLoader nativeLibraryLoader;
    
    final String nativeLibraryName;
    
    @Nullable
    Loggable loggable;
    
    @Nullable
    Logging.Severity loggableSeverity;
    
    private InitializationOptions(Context applicationContext, String fieldTrials, boolean enableInternalTracer, NativeLibraryLoader nativeLibraryLoader, String nativeLibraryName, @Nullable Loggable loggable, @Nullable Logging.Severity loggableSeverity) {
      this.applicationContext = applicationContext;
      this.fieldTrials = fieldTrials;
      this.enableInternalTracer = enableInternalTracer;
      this.nativeLibraryLoader = nativeLibraryLoader;
      this.nativeLibraryName = nativeLibraryName;
      this.loggable = loggable;
      this.loggableSeverity = loggableSeverity;
    }
    
    public static Builder builder(Context applicationContext) {
      return new Builder(applicationContext);
    }
    
    public static class Builder {
      private final Context applicationContext;
      
      private String fieldTrials = "";
      
      private boolean enableInternalTracer;
      
      private NativeLibraryLoader nativeLibraryLoader = new NativeLibrary.DefaultLoader();
      
      private String nativeLibraryName = "jingle_peerconnection_so";
      
      @Nullable
      private Loggable loggable;
      
      @Nullable
      private Logging.Severity loggableSeverity;
      
      Builder(Context applicationContext) {
        this.applicationContext = applicationContext;
      }
      
      public Builder setFieldTrials(String fieldTrials) {
        this.fieldTrials = fieldTrials;
        return this;
      }
      
      public Builder setEnableInternalTracer(boolean enableInternalTracer) {
        this.enableInternalTracer = enableInternalTracer;
        return this;
      }
      
      public Builder setNativeLibraryLoader(NativeLibraryLoader nativeLibraryLoader) {
        this.nativeLibraryLoader = nativeLibraryLoader;
        return this;
      }
      
      public Builder setNativeLibraryName(String nativeLibraryName) {
        this.nativeLibraryName = nativeLibraryName;
        return this;
      }
      
      public Builder setInjectableLogger(Loggable loggable, Logging.Severity severity) {
        this.loggable = loggable;
        this.loggableSeverity = severity;
        return this;
      }
      
      public PeerConnectionFactory.InitializationOptions createInitializationOptions() {
        return new PeerConnectionFactory.InitializationOptions(this.applicationContext, this.fieldTrials, this.enableInternalTracer, this.nativeLibraryLoader, this.nativeLibraryName, this.loggable, this.loggableSeverity);
      }
    }
  }
  
  public static class Options {
    static final int ADAPTER_TYPE_UNKNOWN = 0;
    
    static final int ADAPTER_TYPE_ETHERNET = 1;
    
    static final int ADAPTER_TYPE_WIFI = 2;
    
    static final int ADAPTER_TYPE_CELLULAR = 4;
    
    static final int ADAPTER_TYPE_VPN = 8;
    
    static final int ADAPTER_TYPE_LOOPBACK = 16;
    
    static final int ADAPTER_TYPE_ANY = 32;
    
    public int networkIgnoreMask;
    
    public boolean disableEncryption;
    
    public boolean disableNetworkMonitor;
    
    @CalledByNative("Options")
    int getNetworkIgnoreMask() {
      return this.networkIgnoreMask;
    }
    
    @CalledByNative("Options")
    boolean getDisableEncryption() {
      return this.disableEncryption;
    }
    
    @CalledByNative("Options")
    boolean getDisableNetworkMonitor() {
      return this.disableNetworkMonitor;
    }
  }
  
  public static class Builder {
    @Nullable
    private PeerConnectionFactory.Options options;
    
    @Nullable
    private AudioDeviceModule audioDeviceModule;
    
    private AudioEncoderFactoryFactory audioEncoderFactoryFactory = new BuiltinAudioEncoderFactoryFactory();
    
    private AudioDecoderFactoryFactory audioDecoderFactoryFactory = new BuiltinAudioDecoderFactoryFactory();
    
    @Nullable
    private VideoEncoderFactory videoEncoderFactory;
    
    @Nullable
    private VideoDecoderFactory videoDecoderFactory;
    
    @Nullable
    private AudioProcessingFactory audioProcessingFactory;
    
    @Nullable
    private FecControllerFactoryFactoryInterface fecControllerFactoryFactory;
    
    @Nullable
    private NetworkControllerFactoryFactory networkControllerFactoryFactory;
    
    @Nullable
    private NetworkStatePredictorFactoryFactory networkStatePredictorFactoryFactory;
    
    @Nullable
    private MediaTransportFactoryFactory mediaTransportFactoryFactory;
    
    @Nullable
    private NetEqFactoryFactory neteqFactoryFactory;
    
    public Builder setOptions(PeerConnectionFactory.Options options) {
      this.options = options;
      return this;
    }
    
    public Builder setAudioDeviceModule(AudioDeviceModule audioDeviceModule) {
      this.audioDeviceModule = audioDeviceModule;
      return this;
    }
    
    public Builder setAudioEncoderFactoryFactory(AudioEncoderFactoryFactory audioEncoderFactoryFactory) {
      if (audioEncoderFactoryFactory == null)
        throw new IllegalArgumentException("PeerConnectionFactory.Builder does not accept a null AudioEncoderFactoryFactory."); 
      this.audioEncoderFactoryFactory = audioEncoderFactoryFactory;
      return this;
    }
    
    public Builder setAudioDecoderFactoryFactory(AudioDecoderFactoryFactory audioDecoderFactoryFactory) {
      if (audioDecoderFactoryFactory == null)
        throw new IllegalArgumentException("PeerConnectionFactory.Builder does not accept a null AudioDecoderFactoryFactory."); 
      this.audioDecoderFactoryFactory = audioDecoderFactoryFactory;
      return this;
    }
    
    public Builder setVideoEncoderFactory(VideoEncoderFactory videoEncoderFactory) {
      this.videoEncoderFactory = videoEncoderFactory;
      return this;
    }
    
    public Builder setVideoDecoderFactory(VideoDecoderFactory videoDecoderFactory) {
      this.videoDecoderFactory = videoDecoderFactory;
      return this;
    }
    
    public Builder setAudioProcessingFactory(AudioProcessingFactory audioProcessingFactory) {
      if (audioProcessingFactory == null)
        throw new NullPointerException("PeerConnectionFactory builder does not accept a null AudioProcessingFactory."); 
      this.audioProcessingFactory = audioProcessingFactory;
      return this;
    }
    
    public Builder setFecControllerFactoryFactoryInterface(FecControllerFactoryFactoryInterface fecControllerFactoryFactory) {
      this.fecControllerFactoryFactory = fecControllerFactoryFactory;
      return this;
    }
    
    public Builder setNetworkControllerFactoryFactory(NetworkControllerFactoryFactory networkControllerFactoryFactory) {
      this.networkControllerFactoryFactory = networkControllerFactoryFactory;
      return this;
    }
    
    public Builder setNetworkStatePredictorFactoryFactory(NetworkStatePredictorFactoryFactory networkStatePredictorFactoryFactory) {
      this.networkStatePredictorFactoryFactory = networkStatePredictorFactoryFactory;
      return this;
    }
    
    public Builder setMediaTransportFactoryFactory(MediaTransportFactoryFactory mediaTransportFactoryFactory) {
      this.mediaTransportFactoryFactory = mediaTransportFactoryFactory;
      return this;
    }
    
    public Builder setNetEqFactoryFactory(NetEqFactoryFactory neteqFactoryFactory) {
      this.neteqFactoryFactory = neteqFactoryFactory;
      return this;
    }
    
    public PeerConnectionFactory createPeerConnectionFactory() {
      PeerConnectionFactory.checkInitializeHasBeenCalled();
      if (this.audioDeviceModule == null)
        this
          .audioDeviceModule = JavaAudioDeviceModule.builder(ContextUtils.getApplicationContext()).createAudioDeviceModule(); 
      return PeerConnectionFactory.nativeCreatePeerConnectionFactory(ContextUtils.getApplicationContext(), this.options, this.audioDeviceModule
          .getNativeAudioDeviceModulePointer(), this.audioEncoderFactoryFactory
          .createNativeAudioEncoderFactory(), this.audioDecoderFactoryFactory
          .createNativeAudioDecoderFactory(), this.videoEncoderFactory, this.videoDecoderFactory, 
          
          (this.audioProcessingFactory == null) ? 0L : this.audioProcessingFactory.createNative(), 
          (this.fecControllerFactoryFactory == null) ? 0L : this.fecControllerFactoryFactory.createNative(), 
          (this.networkControllerFactoryFactory == null) ? 
          0L : 
          this.networkControllerFactoryFactory.createNativeNetworkControllerFactory(), 
          (this.networkStatePredictorFactoryFactory == null) ? 
          0L : 
          this.networkStatePredictorFactoryFactory.createNativeNetworkStatePredictorFactory(), 
          (this.mediaTransportFactoryFactory == null) ? 
          0L : 
          this.mediaTransportFactoryFactory.createNativeMediaTransportFactory(), 
          (this.neteqFactoryFactory == null) ? 0L : this.neteqFactoryFactory.createNativeNetEqFactory());
    }
    
    private Builder() {}
  }
  
  public static Builder builder() {
    return new Builder();
  }
  
  public static void initialize(InitializationOptions options) {
    ContextUtils.initialize(options.applicationContext);
    NativeLibrary.initialize(options.nativeLibraryLoader, options.nativeLibraryName);
    nativeInitializeAndroidGlobals();
    nativeInitializeFieldTrials(options.fieldTrials);
    if (options.enableInternalTracer && !internalTracerInitialized)
      initializeInternalTracer(); 
    if (options.loggable != null) {
      Logging.injectLoggable(options.loggable, options.loggableSeverity);
      nativeInjectLoggable(new JNILogging(options.loggable), options.loggableSeverity.ordinal());
    } else {
      Logging.d("PeerConnectionFactory", "PeerConnectionFactory was initialized without an injected Loggable. Any existing Loggable will be deleted.");
      Logging.deleteInjectedLoggable();
      nativeDeleteLoggable();
    } 
  }
  
  private static void checkInitializeHasBeenCalled() {
    if (!NativeLibrary.isLoaded() || ContextUtils.getApplicationContext() == null)
      throw new IllegalStateException("PeerConnectionFactory.initialize was not called before creating a PeerConnectionFactory."); 
  }
  
  private static void initializeInternalTracer() {
    internalTracerInitialized = true;
    nativeInitializeInternalTracer();
  }
  
  public static void shutdownInternalTracer() {
    internalTracerInitialized = false;
    nativeShutdownInternalTracer();
  }
  
  @Deprecated
  public static void initializeFieldTrials(String fieldTrialsInitString) {
    nativeInitializeFieldTrials(fieldTrialsInitString);
  }
  
  public static String fieldTrialsFindFullName(String name) {
    return NativeLibrary.isLoaded() ? nativeFindFieldTrialsFullName(name) : "";
  }
  
  public static boolean startInternalTracingCapture(String tracingFilename) {
    return nativeStartInternalTracingCapture(tracingFilename);
  }
  
  public static void stopInternalTracingCapture() {
    nativeStopInternalTracingCapture();
  }
  
  @CalledByNative
  PeerConnectionFactory(long nativeFactory) {
    checkInitializeHasBeenCalled();
    if (nativeFactory == 0L)
      throw new RuntimeException("Failed to initialize PeerConnectionFactory!"); 
    this.nativeFactory = nativeFactory;
  }
  
  @Nullable
  PeerConnection createPeerConnectionInternal(PeerConnection.RTCConfiguration rtcConfig, MediaConstraints constraints, PeerConnection.Observer observer, SSLCertificateVerifier sslCertificateVerifier) {
    checkPeerConnectionFactoryExists();
    long nativeObserver = PeerConnection.createNativePeerConnectionObserver(observer);
    if (nativeObserver == 0L)
      return null; 
    long nativePeerConnection = nativeCreatePeerConnection(this.nativeFactory, rtcConfig, constraints, nativeObserver, sslCertificateVerifier);
    if (nativePeerConnection == 0L)
      return null; 
    return new PeerConnection(nativePeerConnection);
  }
  
  @Deprecated
  @Nullable
  public PeerConnection createPeerConnection(PeerConnection.RTCConfiguration rtcConfig, MediaConstraints constraints, PeerConnection.Observer observer) {
    return createPeerConnectionInternal(rtcConfig, constraints, observer, null);
  }
  
  @Deprecated
  @Nullable
  public PeerConnection createPeerConnection(List<PeerConnection.IceServer> iceServers, MediaConstraints constraints, PeerConnection.Observer observer) {
    PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
    return createPeerConnection(rtcConfig, constraints, observer);
  }
  
  @Nullable
  public PeerConnection createPeerConnection(List<PeerConnection.IceServer> iceServers, PeerConnection.Observer observer) {
    PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
    return createPeerConnection(rtcConfig, observer);
  }
  
  @Nullable
  public PeerConnection createPeerConnection(PeerConnection.RTCConfiguration rtcConfig, PeerConnection.Observer observer) {
    return createPeerConnection(rtcConfig, (MediaConstraints)null, observer);
  }
  
  @Nullable
  public PeerConnection createPeerConnection(PeerConnection.RTCConfiguration rtcConfig, PeerConnectionDependencies dependencies) {
    return createPeerConnectionInternal(rtcConfig, null, dependencies
        .getObserver(), dependencies.getSSLCertificateVerifier());
  }
  
  public MediaStream createLocalMediaStream(String label) {
    checkPeerConnectionFactoryExists();
    return new MediaStream(nativeCreateLocalMediaStream(this.nativeFactory, label));
  }
  
  public VideoSource createVideoSource(boolean isScreencast, boolean alignTimestamps) {
    checkPeerConnectionFactoryExists();
    return new VideoSource(nativeCreateVideoSource(this.nativeFactory, isScreencast, alignTimestamps));
  }
  
  public VideoSource createVideoSource(boolean isScreencast) {
    return createVideoSource(isScreencast, true);
  }
  
  public VideoTrack createVideoTrack(String id, VideoSource source) {
    checkPeerConnectionFactoryExists();
    return new VideoTrack(
        nativeCreateVideoTrack(this.nativeFactory, id, source.getNativeVideoTrackSource()));
  }
  
  public AudioSource createAudioSource(MediaConstraints constraints) {
    checkPeerConnectionFactoryExists();
    return new AudioSource(nativeCreateAudioSource(this.nativeFactory, constraints));
  }
  
  public AudioTrack createAudioTrack(String id, AudioSource source) {
    checkPeerConnectionFactoryExists();
    return new AudioTrack(nativeCreateAudioTrack(this.nativeFactory, id, source.getNativeAudioSource()));
  }
  
  public boolean startAecDump(int file_descriptor, int filesize_limit_bytes) {
    checkPeerConnectionFactoryExists();
    return nativeStartAecDump(this.nativeFactory, file_descriptor, filesize_limit_bytes);
  }
  
  public void stopAecDump() {
    checkPeerConnectionFactoryExists();
    nativeStopAecDump(this.nativeFactory);
  }
  
  public void dispose() {
    checkPeerConnectionFactoryExists();
    nativeFreeFactory(this.nativeFactory);
    this.networkThread = null;
    this.workerThread = null;
    this.signalingThread = null;
    this.nativeFactory = 0L;
  }
  
  public long getNativePeerConnectionFactory() {
    checkPeerConnectionFactoryExists();
    return nativeGetNativePeerConnectionFactory(this.nativeFactory);
  }
  
  public long getNativeOwnedFactoryAndThreads() {
    checkPeerConnectionFactoryExists();
    return this.nativeFactory;
  }
  
  private void checkPeerConnectionFactoryExists() {
    if (this.nativeFactory == 0L)
      throw new IllegalStateException("PeerConnectionFactory has been disposed."); 
  }
  
  private static void printStackTrace(@Nullable ThreadInfo threadInfo, boolean printNativeStackTrace) {
    if (threadInfo == null)
      return; 
    String threadName = threadInfo.thread.getName();
    StackTraceElement[] stackTraces = threadInfo.thread.getStackTrace();
    if (stackTraces.length > 0) {
      Logging.w("PeerConnectionFactory", threadName + " stacktrace:");
      for (StackTraceElement stackTrace : stackTraces)
        Logging.w("PeerConnectionFactory", stackTrace.toString()); 
    } 
    if (printNativeStackTrace) {
      Logging.w("PeerConnectionFactory", "*** *** *** *** *** *** *** *** *** *** *** *** *** *** *** ***");
      Logging.w("PeerConnectionFactory", "pid: " + 
          Process.myPid() + ", tid: " + threadInfo.tid + ", name: " + threadName + "  >>> WebRTC <<<");
      nativePrintStackTrace(threadInfo.tid);
    } 
  }
  
  @Deprecated
  public static void printStackTraces() {
    printStackTrace(staticNetworkThread, false);
    printStackTrace(staticWorkerThread, false);
    printStackTrace(staticSignalingThread, false);
  }
  
  public void printInternalStackTraces(boolean printNativeStackTraces) {
    printStackTrace(this.signalingThread, printNativeStackTraces);
    printStackTrace(this.workerThread, printNativeStackTraces);
    printStackTrace(this.networkThread, printNativeStackTraces);
    if (printNativeStackTraces)
      nativePrintStackTracesOfRegisteredThreads(); 
  }
  
  @CalledByNative
  private void onNetworkThreadReady() {
    this.networkThread = ThreadInfo.getCurrent();
    staticNetworkThread = this.networkThread;
    Logging.d("PeerConnectionFactory", "onNetworkThreadReady");
  }
  
  @CalledByNative
  private void onWorkerThreadReady() {
    this.workerThread = ThreadInfo.getCurrent();
    staticWorkerThread = this.workerThread;
    Logging.d("PeerConnectionFactory", "onWorkerThreadReady");
  }
  
  @CalledByNative
  private void onSignalingThreadReady() {
    this.signalingThread = ThreadInfo.getCurrent();
    staticSignalingThread = this.signalingThread;
    Logging.d("PeerConnectionFactory", "onSignalingThreadReady");
  }
  
  private static native void nativeInitializeAndroidGlobals();
  
  private static native void nativeInitializeFieldTrials(String paramString);
  
  private static native String nativeFindFieldTrialsFullName(String paramString);
  
  private static native void nativeInitializeInternalTracer();
  
  private static native void nativeShutdownInternalTracer();
  
  private static native boolean nativeStartInternalTracingCapture(String paramString);
  
  private static native void nativeStopInternalTracingCapture();
  
  private static native PeerConnectionFactory nativeCreatePeerConnectionFactory(Context paramContext, Options paramOptions, long paramLong1, long paramLong2, long paramLong3, VideoEncoderFactory paramVideoEncoderFactory, VideoDecoderFactory paramVideoDecoderFactory, long paramLong4, long paramLong5, long paramLong6, long paramLong7, long paramLong8, long paramLong9);
  
  private static native long nativeCreatePeerConnection(long paramLong1, PeerConnection.RTCConfiguration paramRTCConfiguration, MediaConstraints paramMediaConstraints, long paramLong2, SSLCertificateVerifier paramSSLCertificateVerifier);
  
  private static native long nativeCreateLocalMediaStream(long paramLong, String paramString);
  
  private static native long nativeCreateVideoSource(long paramLong, boolean paramBoolean1, boolean paramBoolean2);
  
  private static native long nativeCreateVideoTrack(long paramLong1, String paramString, long paramLong2);
  
  private static native long nativeCreateAudioSource(long paramLong, MediaConstraints paramMediaConstraints);
  
  private static native long nativeCreateAudioTrack(long paramLong1, String paramString, long paramLong2);
  
  private static native boolean nativeStartAecDump(long paramLong, int paramInt1, int paramInt2);
  
  private static native void nativeStopAecDump(long paramLong);
  
  private static native void nativeFreeFactory(long paramLong);
  
  private static native long nativeGetNativePeerConnectionFactory(long paramLong);
  
  private static native void nativeInjectLoggable(JNILogging paramJNILogging, int paramInt);
  
  private static native void nativeDeleteLoggable();
  
  private static native void nativePrintStackTrace(int paramInt);
  
  private static native void nativePrintStackTracesOfRegisteredThreads();
}
