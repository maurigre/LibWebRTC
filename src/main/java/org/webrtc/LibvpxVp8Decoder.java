package org.webrtc;

public class LibvpxVp8Decoder extends WrappedNativeVideoDecoder {
  public long createNativeVideoDecoder() {
    return nativeCreateDecoder();
  }
  
  static native long nativeCreateDecoder();
}
