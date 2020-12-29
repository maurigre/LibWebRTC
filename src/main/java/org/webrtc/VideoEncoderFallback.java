package org.webrtc;

public class VideoEncoderFallback extends WrappedNativeVideoEncoder {
  private final VideoEncoder fallback;
  
  private final VideoEncoder primary;
  
  public VideoEncoderFallback(VideoEncoder fallback, VideoEncoder primary) {
    this.fallback = fallback;
    this.primary = primary;
  }
  
  public long createNativeVideoEncoder() {
    return nativeCreateEncoder(this.fallback, this.primary);
  }
  
  public boolean isHardwareEncoder() {
    return this.primary.isHardwareEncoder();
  }
  
  private static native long nativeCreateEncoder(VideoEncoder paramVideoEncoder1, VideoEncoder paramVideoEncoder2);
}
