package org.webrtc;

public class CallSessionFileRotatingLogSink {
  private long nativeSink;
  
  public static byte[] getLogData(String dirPath) {
    if (dirPath == null)
      throw new IllegalArgumentException("dirPath may not be null."); 
    return nativeGetLogData(dirPath);
  }
  
  public CallSessionFileRotatingLogSink(String dirPath, int maxFileSize, Logging.Severity severity) {
    if (dirPath == null)
      throw new IllegalArgumentException("dirPath may not be null."); 
    this.nativeSink = nativeAddSink(dirPath, maxFileSize, severity.ordinal());
  }
  
  public void dispose() {
    if (this.nativeSink != 0L) {
      nativeDeleteSink(this.nativeSink);
      this.nativeSink = 0L;
    } 
  }
  
  private static native long nativeAddSink(String paramString, int paramInt1, int paramInt2);
  
  private static native void nativeDeleteSink(long paramLong);
  
  private static native byte[] nativeGetLogData(String paramString);
}
