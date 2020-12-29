package org.webrtc;

import android.support.annotation.Nullable;

public interface VideoDecoderFactory {
  @Deprecated
  @Nullable
  default VideoDecoder createDecoder(String codecType) {
    throw new UnsupportedOperationException("Deprecated and not implemented.");
  }
  
  @Nullable
  @CalledByNative
  default VideoDecoder createDecoder(VideoCodecInfo info) {
    return createDecoder(info.getName());
  }
  
  @CalledByNative
  default VideoCodecInfo[] getSupportedCodecs() {
    return new VideoCodecInfo[0];
  }
}
