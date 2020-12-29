package org.webrtc;

public class TurnCustomizer {
  private long nativeTurnCustomizer;
  
  public TurnCustomizer(long nativeTurnCustomizer) {
    this.nativeTurnCustomizer = nativeTurnCustomizer;
  }
  
  public void dispose() {
    checkTurnCustomizerExists();
    nativeFreeTurnCustomizer(this.nativeTurnCustomizer);
    this.nativeTurnCustomizer = 0L;
  }
  
  private static native void nativeFreeTurnCustomizer(long paramLong);
  
  @CalledByNative
  long getNativeTurnCustomizer() {
    checkTurnCustomizerExists();
    return this.nativeTurnCustomizer;
  }
  
  private void checkTurnCustomizerExists() {
    if (this.nativeTurnCustomizer == 0L)
      throw new IllegalStateException("TurnCustomizer has been disposed."); 
  }
}
