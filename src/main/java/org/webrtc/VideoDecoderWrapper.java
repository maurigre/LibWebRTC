package org.webrtc;

class VideoDecoderWrapper {
  @CalledByNative
  static VideoDecoder.Callback createDecoderCallback(long nativeDecoder) {
    return (frame, decodeTimeMs, qp) -> nativeOnDecodedFrame(nativeDecoder, frame, decodeTimeMs, qp);
  }
  
  private static native void nativeOnDecodedFrame(long paramLong, VideoFrame paramVideoFrame, Integer paramInteger1, Integer paramInteger2);
}
