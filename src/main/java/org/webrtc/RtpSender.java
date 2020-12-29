package org.webrtc;

import android.support.annotation.Nullable;
import java.util.List;

public class RtpSender {
  private long nativeRtpSender;
  
  @Nullable
  private MediaStreamTrack cachedTrack;
  
  private boolean ownsTrack = true;
  
  @Nullable
  private final DtmfSender dtmfSender;
  
  @CalledByNative
  public RtpSender(long nativeRtpSender) {
    this.nativeRtpSender = nativeRtpSender;
    long nativeTrack = nativeGetTrack(nativeRtpSender);
    this.cachedTrack = MediaStreamTrack.createMediaStreamTrack(nativeTrack);
    long nativeDtmfSender = nativeGetDtmfSender(nativeRtpSender);
    this.dtmfSender = (nativeDtmfSender != 0L) ? new DtmfSender(nativeDtmfSender) : null;
  }
  
  public boolean setTrack(@Nullable MediaStreamTrack track, boolean takeOwnership) {
    checkRtpSenderExists();
    if (!nativeSetTrack(this.nativeRtpSender, (track == null) ? 0L : track.getNativeMediaStreamTrack()))
      return false; 
    if (this.cachedTrack != null && this.ownsTrack)
      this.cachedTrack.dispose(); 
    this.cachedTrack = track;
    this.ownsTrack = takeOwnership;
    return true;
  }
  
  @Nullable
  public MediaStreamTrack track() {
    return this.cachedTrack;
  }
  
  public void setStreams(List<String> streamIds) {
    checkRtpSenderExists();
    nativeSetStreams(this.nativeRtpSender, streamIds);
  }
  
  public List<String> getStreams() {
    checkRtpSenderExists();
    return nativeGetStreams(this.nativeRtpSender);
  }
  
  public boolean setParameters(RtpParameters parameters) {
    checkRtpSenderExists();
    return nativeSetParameters(this.nativeRtpSender, parameters);
  }
  
  public RtpParameters getParameters() {
    checkRtpSenderExists();
    return nativeGetParameters(this.nativeRtpSender);
  }
  
  public String id() {
    checkRtpSenderExists();
    return nativeGetId(this.nativeRtpSender);
  }
  
  @Nullable
  public DtmfSender dtmf() {
    return this.dtmfSender;
  }
  
  public void setFrameEncryptor(FrameEncryptor frameEncryptor) {
    checkRtpSenderExists();
    nativeSetFrameEncryptor(this.nativeRtpSender, frameEncryptor.getNativeFrameEncryptor());
  }
  
  public void dispose() {
    checkRtpSenderExists();
    if (this.dtmfSender != null)
      this.dtmfSender.dispose(); 
    if (this.cachedTrack != null && this.ownsTrack)
      this.cachedTrack.dispose(); 
    JniCommon.nativeReleaseRef(this.nativeRtpSender);
    this.nativeRtpSender = 0L;
  }
  
  long getNativeRtpSender() {
    checkRtpSenderExists();
    return this.nativeRtpSender;
  }
  
  private void checkRtpSenderExists() {
    if (this.nativeRtpSender == 0L)
      throw new IllegalStateException("RtpSender has been disposed."); 
  }
  
  private static native boolean nativeSetTrack(long paramLong1, long paramLong2);
  
  private static native long nativeGetTrack(long paramLong);
  
  private static native void nativeSetStreams(long paramLong, List<String> paramList);
  
  private static native List<String> nativeGetStreams(long paramLong);
  
  private static native long nativeGetDtmfSender(long paramLong);
  
  private static native boolean nativeSetParameters(long paramLong, RtpParameters paramRtpParameters);
  
  private static native RtpParameters nativeGetParameters(long paramLong);
  
  private static native String nativeGetId(long paramLong);
  
  private static native void nativeSetFrameEncryptor(long paramLong1, long paramLong2);
}
