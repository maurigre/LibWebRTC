package org.webrtc;

public interface RTCStatsCollectorCallback {
  @CalledByNative
  void onStatsDelivered(RTCStatsReport paramRTCStatsReport);
}
