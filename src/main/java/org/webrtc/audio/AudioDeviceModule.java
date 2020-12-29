package org.webrtc.audio;

public interface AudioDeviceModule {
  long getNativeAudioDeviceModulePointer();
  
  void release();
  
  void setSpeakerMute(boolean paramBoolean);
  
  void setMicrophoneMute(boolean paramBoolean);
}
