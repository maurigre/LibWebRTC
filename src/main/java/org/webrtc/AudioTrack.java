package org.webrtc;

public class AudioTrack extends MediaStreamTrack {
  public AudioTrack(long nativeTrack) {
    super(nativeTrack);
  }
  
  public void setVolume(double volume) {
    nativeSetVolume(getNativeAudioTrack(), volume);
  }
  
  long getNativeAudioTrack() {
    return getNativeMediaStreamTrack();
  }
  
  private static native void nativeSetVolume(long paramLong, double paramDouble);
}
