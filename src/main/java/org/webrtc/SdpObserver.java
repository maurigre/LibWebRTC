package org.webrtc;

public interface SdpObserver {
  @CalledByNative
  void onCreateSuccess(SessionDescription paramSessionDescription);
  
  @CalledByNative
  void onSetSuccess();
  
  @CalledByNative
  void onCreateFailure(String paramString);
  
  @CalledByNative
  void onSetFailure(String paramString);
}
