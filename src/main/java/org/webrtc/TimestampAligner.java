package org.webrtc;

public class TimestampAligner {
  public static long getRtcTimeNanos() {
    return nativeRtcTimeNanos();
  }
  
  private volatile long nativeTimestampAligner = nativeCreateTimestampAligner();
  
  public long translateTimestamp(long cameraTimeNs) {
    checkNativeAlignerExists();
    return nativeTranslateTimestamp(this.nativeTimestampAligner, cameraTimeNs);
  }
  
  public void dispose() {
    checkNativeAlignerExists();
    nativeReleaseTimestampAligner(this.nativeTimestampAligner);
    this.nativeTimestampAligner = 0L;
  }
  
  private void checkNativeAlignerExists() {
    if (this.nativeTimestampAligner == 0L)
      throw new IllegalStateException("TimestampAligner has been disposed."); 
  }
  
  private static native long nativeRtcTimeNanos();
  
  private static native long nativeCreateTimestampAligner();
  
  private static native void nativeReleaseTimestampAligner(long paramLong);
  
  private static native long nativeTranslateTimestamp(long paramLong1, long paramLong2);
}
