package org.webrtc;

public interface CapturerObserver {
  void onCapturerStarted(boolean paramBoolean);
  
  void onCapturerStopped();
  
  void onFrameCaptured(VideoFrame paramVideoFrame);
}
