package org.webrtc;

public final class CandidatePairChangeEvent {
  public final IceCandidate local;
  
  public final IceCandidate remote;
  
  public final int lastDataReceivedMs;
  
  public final String reason;
  
  @CalledByNative
  CandidatePairChangeEvent(IceCandidate local, IceCandidate remote, int lastDataReceivedMs, String reason) {
    this.local = local;
    this.remote = remote;
    this.lastDataReceivedMs = lastDataReceivedMs;
    this.reason = reason;
  }
}
