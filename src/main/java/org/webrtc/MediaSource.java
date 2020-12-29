package org.webrtc;

public class MediaSource {
  private final RefCountDelegate refCountDelegate;
  
  private long nativeSource;
  
  public enum State {
    INITIALIZING, LIVE, ENDED, MUTED;
    
    @CalledByNative("State")
    static State fromNativeIndex(int nativeIndex) {
      return values()[nativeIndex];
    }
  }
  
  public MediaSource(long nativeSource) {
    this.refCountDelegate = new RefCountDelegate(() -> JniCommon.nativeReleaseRef(nativeSource));
    this.nativeSource = nativeSource;
  }
  
  public State state() {
    checkMediaSourceExists();
    return nativeGetState(this.nativeSource);
  }
  
  public void dispose() {
    checkMediaSourceExists();
    this.refCountDelegate.release();
    this.nativeSource = 0L;
  }
  
  protected long getNativeMediaSource() {
    checkMediaSourceExists();
    return this.nativeSource;
  }
  
  void runWithReference(Runnable runnable) {
    if (this.refCountDelegate.safeRetain())
      try {
        runnable.run();
      } finally {
        this.refCountDelegate.release();
      }  
  }
  
  private void checkMediaSourceExists() {
    if (this.nativeSource == 0L)
      throw new IllegalStateException("MediaSource has been disposed."); 
  }
  
  private static native State nativeGetState(long paramLong);
}
