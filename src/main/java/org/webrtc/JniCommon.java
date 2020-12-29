package org.webrtc;

import java.nio.ByteBuffer;

public class JniCommon {
  public static native void nativeAddRef(long paramLong);
  
  public static native void nativeReleaseRef(long paramLong);
  
  public static native ByteBuffer nativeAllocateByteBuffer(int paramInt);
  
  public static native void nativeFreeByteBuffer(ByteBuffer paramByteBuffer);
}
