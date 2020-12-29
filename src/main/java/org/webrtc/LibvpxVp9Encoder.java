package org.webrtc;

public class LibvpxVp9Encoder extends WrappedNativeVideoEncoder {
  public long createNativeVideoEncoder() {
    return nativeCreateEncoder();
  }
  
  static native long nativeCreateEncoder();
  
  public boolean isHardwareEncoder() {
    return false;
  }
  
  static native boolean nativeIsSupported();
}
