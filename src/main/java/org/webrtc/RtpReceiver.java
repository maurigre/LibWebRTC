package org.webrtc;

import android.support.annotation.Nullable;

public class RtpReceiver {
  private long nativeRtpReceiver;
  
  private long nativeObserver;
  
  @Nullable
  private MediaStreamTrack cachedTrack;
  
  @CalledByNative
  public RtpReceiver(long nativeRtpReceiver) {
    this.nativeRtpReceiver = nativeRtpReceiver;
    long nativeTrack = nativeGetTrack(nativeRtpReceiver);
    this.cachedTrack = MediaStreamTrack.createMediaStreamTrack(nativeTrack);
  }
  
  @Nullable
  public MediaStreamTrack track() {
    return this.cachedTrack;
  }
  
  public RtpParameters getParameters() {
    checkRtpReceiverExists();
    return nativeGetParameters(this.nativeRtpReceiver);
  }
  
  public String id() {
    checkRtpReceiverExists();
    return nativeGetId(this.nativeRtpReceiver);
  }
  
  @CalledByNative
  public void dispose() {
    checkRtpReceiverExists();
    this.cachedTrack.dispose();
    if (this.nativeObserver != 0L) {
      nativeUnsetObserver(this.nativeRtpReceiver, this.nativeObserver);
      this.nativeObserver = 0L;
    } 
    JniCommon.nativeReleaseRef(this.nativeRtpReceiver);
    this.nativeRtpReceiver = 0L;
  }
  
  public void SetObserver(Observer observer) {
    checkRtpReceiverExists();
    if (this.nativeObserver != 0L)
      nativeUnsetObserver(this.nativeRtpReceiver, this.nativeObserver); 
    this.nativeObserver = nativeSetObserver(this.nativeRtpReceiver, observer);
  }
  
  public void setFrameDecryptor(FrameDecryptor frameDecryptor) {
    checkRtpReceiverExists();
    nativeSetFrameDecryptor(this.nativeRtpReceiver, frameDecryptor.getNativeFrameDecryptor());
  }
  
  private void checkRtpReceiverExists() {
    if (this.nativeRtpReceiver == 0L)
      throw new IllegalStateException("RtpReceiver has been disposed."); 
  }
  
  private static native long nativeGetTrack(long paramLong);
  
  private static native RtpParameters nativeGetParameters(long paramLong);
  
  private static native String nativeGetId(long paramLong);
  
  private static native long nativeSetObserver(long paramLong, Observer paramObserver);
  
  private static native void nativeUnsetObserver(long paramLong1, long paramLong2);
  
  private static native void nativeSetFrameDecryptor(long paramLong1, long paramLong2);
  
  public static interface Observer {
    @CalledByNative("Observer")
    void onFirstPacketReceived(MediaStreamTrack.MediaType param1MediaType);
  }
}
