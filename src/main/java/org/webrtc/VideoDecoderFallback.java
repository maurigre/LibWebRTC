package org.webrtc;

public class VideoDecoderFallback extends WrappedNativeVideoDecoder {
  private final VideoDecoder fallback;
  
  private final VideoDecoder primary;
  
  public VideoDecoderFallback(VideoDecoder fallback, VideoDecoder primary) {
    this.fallback = fallback;
    this.primary = primary;
  }
  
  public long createNativeVideoDecoder() {
    return nativeCreateDecoder(this.fallback, this.primary);
  }
  
  private static native long nativeCreateDecoder(VideoDecoder paramVideoDecoder1, VideoDecoder paramVideoDecoder2);
}
