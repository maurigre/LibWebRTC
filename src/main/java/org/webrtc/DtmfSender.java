package org.webrtc;

public class DtmfSender {
  private long nativeDtmfSender;
  
  public DtmfSender(long nativeDtmfSender) {
    this.nativeDtmfSender = nativeDtmfSender;
  }
  
  public boolean canInsertDtmf() {
    checkDtmfSenderExists();
    return nativeCanInsertDtmf(this.nativeDtmfSender);
  }
  
  public boolean insertDtmf(String tones, int duration, int interToneGap) {
    checkDtmfSenderExists();
    return nativeInsertDtmf(this.nativeDtmfSender, tones, duration, interToneGap);
  }
  
  public String tones() {
    checkDtmfSenderExists();
    return nativeTones(this.nativeDtmfSender);
  }
  
  public int duration() {
    checkDtmfSenderExists();
    return nativeDuration(this.nativeDtmfSender);
  }
  
  public int interToneGap() {
    checkDtmfSenderExists();
    return nativeInterToneGap(this.nativeDtmfSender);
  }
  
  public void dispose() {
    checkDtmfSenderExists();
    JniCommon.nativeReleaseRef(this.nativeDtmfSender);
    this.nativeDtmfSender = 0L;
  }
  
  private void checkDtmfSenderExists() {
    if (this.nativeDtmfSender == 0L)
      throw new IllegalStateException("DtmfSender has been disposed."); 
  }
  
  private static native boolean nativeCanInsertDtmf(long paramLong);
  
  private static native boolean nativeInsertDtmf(long paramLong, String paramString, int paramInt1, int paramInt2);
  
  private static native String nativeTones(long paramLong);
  
  private static native int nativeDuration(long paramLong);
  
  private static native int nativeInterToneGap(long paramLong);
}
