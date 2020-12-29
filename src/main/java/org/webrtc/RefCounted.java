package org.webrtc;

public interface RefCounted {
  @CalledByNative
  void retain();
  
  @CalledByNative
  void release();
}
